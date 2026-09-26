<script setup lang="ts">
import { invoke } from '@tauri-apps/api/core'
import { onMounted, onUnmounted, ref } from 'vue'

import { users } from '@/helpers/auth'

const API = 'https://mc-social-core.lovable.app/api/public/gc'
const KEY = 'ghost.session'

type Person = { id: string; name: string; username?: string; friendshipId: number; status?: string; image?: string }
const token = ref<string | null>(localStorage.getItem(KEY))
const me = ref<any>(null)
const friends = ref<Person[]>([])
const incoming = ref<Person[]>([])
const outgoing = ref<Person[]>([])
const addName = ref('')
const error = ref('')
const busy = ref(false)
let timer: number | undefined

async function api(path: string, method = 'GET', body?: unknown) {
	const res = await fetch(API + path, {
		method,
		headers: {
			'Content-Type': 'application/json',
			...(token.value ? { Authorization: `Bearer ${token.value}` } : {}),
		},
		body: body ? JSON.stringify(body) : undefined,
	})
	const data = await res.json().catch(() => ({}))
	if (res.status === 401 && token.value) signOut()
	if (!res.ok) throw new Error(data?.message || data?.statusMessage || `Error ${res.status}`)
	return data
}

async function writeModSession() {
	const payload = token.value && me.value
		? JSON.stringify({ token: token.value, userId: me.value.id, mcUuid: me.value.mcUuid ?? me.value.mc_uuid, mcUsername: me.value.mcUsername ?? me.value.name, apiBase: API })
		: null
	try { await invoke('plugin:auth|ghost_write_session', { payload }) } catch { /* mod sync optional */ }
}

async function signIn() {
	busy.value = true
	error.value = ''
	try {
		const list: any[] = await users(false)
		const acc = list.find((u) => u.active) ?? list[0]
		if (!acc) throw new Error('Add a Minecraft account first (top left account menu).')
		const offline = acc.account_type === 'offline' || acc.account_type === 2 || acc.access_token === '0'
		const data = offline
			? await api('/auth/offline', 'POST', { username: acc.profile?.name })
			: await api('/auth/minecraft', 'POST', { token: acc.access_token })
		token.value = data.session.token
		localStorage.setItem(KEY, token.value!)
		me.value = data.user
		await writeModSession()
		await refresh()
	} catch (e: any) {
		error.value = e.message
	} finally {
		busy.value = false
	}
}

function signOut() {
	token.value = null
	me.value = null
	localStorage.removeItem(KEY)
	writeModSession()
}

async function refresh() {
	if (!token.value) return
	try {
		if (!me.value) {
			const s = await api('/auth/get-session')
			if (!s?.user) return signOut()
			me.value = s.user
			await writeModSession()
		}
		const d = await api('/friends')
		friends.value = d.friends ?? []
		incoming.value = d.incoming ?? []
		outgoing.value = d.outgoing ?? []
		api('/presence', 'POST', { mode: 'visible', status: 'online' }).catch(() => {})
	} catch (e: any) {
		error.value = e.message
	}
}

async function act(fn: () => Promise<unknown>) {
	error.value = ''
	try { await fn(); await refresh() } catch (e: any) { error.value = e.message }
}
const add = () => act(async () => { await api('/friends', 'POST', { username: addName.value.trim() }); addName.value = '' })
const respond = (p: Person, action: 'accept' | 'reject') => act(() => api(`/friends/${p.friendshipId}`, 'PATCH', { action }))
const remove = (p: Person) => act(() => api(`/friends/${p.friendshipId}`, 'DELETE'))
const online = (p: Person) => ['online', 'in_game', 'dnd'].includes(p.status ?? '')

onMounted(() => { refresh(); timer = window.setInterval(refresh, 15000) })
onUnmounted(() => clearInterval(timer))
</script>

<template>
	<div class="gs">
		<header>
			<h1>Friends</h1>
			<div v-if="me" class="me">
				<img :src="me.image" alt="" />
				<span>{{ me.name }}</span>
				<button class="ghost" @click="signOut">Sign out</button>
			</div>
		</header>

		<p v-if="error" class="err">{{ error }}</p>

		<section v-if="!token" class="card center">
			<h2>Play with your friends</h2>
			<p>Sign in with your selected Minecraft account (Microsoft or offline) to add friends, see who is online and sync capes with the Ghost mod.</p>
			<button :disabled="busy" @click="signIn">{{ busy ? 'Signing in…' : 'Sign in with Minecraft' }}</button>
		</section>

		<template v-else>
			<form class="card row" @submit.prevent="add">
				<input v-model="addName" placeholder="Minecraft username" />
				<button :disabled="!addName.trim()">Add friend</button>
			</form>

			<section v-if="incoming.length" class="card">
				<h3>Requests</h3>
				<div v-for="p in incoming" :key="p.friendshipId" class="person">
					<span>{{ p.name }}</span>
					<button @click="respond(p, 'accept')">Accept</button>
					<button class="ghost" @click="respond(p, 'reject')">Decline</button>
				</div>
			</section>

			<section class="card">
				<h3>Friends · {{ friends.filter(online).length }} online</h3>
				<p v-if="!friends.length" class="muted">No friends yet.</p>
				<div v-for="p in friends" :key="p.friendshipId" class="person">
					<i :class="['dot', online(p) && 'on']" />
					<span>{{ p.name }}</span>
					<small class="muted">{{ p.status === 'in_game' ? 'In game' : online(p) ? 'Online' : 'Offline' }}</small>
					<button class="ghost" @click="remove(p)">Remove</button>
				</div>
			</section>

			<section v-if="outgoing.length" class="card">
				<h3>Sent</h3>
				<div v-for="p in outgoing" :key="p.friendshipId" class="person">
					<span>{{ p.name }}</span>
					<small class="muted">Pending</small>
					<button class="ghost" @click="remove(p)">Cancel</button>
				</div>
			</section>
		</template>
	</div>
</template>

<style scoped>
.gs { padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; max-width: 760px; }
header { display: flex; align-items: center; justify-content: space-between; }
h1 { margin: 0; font-size: 1.6rem; color: var(--color-contrast); }
h2, h3 { margin: 0 0 .6rem; color: var(--color-contrast); }
.card { background: var(--surface-3); border: 1px solid var(--surface-5); border-radius: 14px; padding: 1rem; }
.center { text-align: center; padding: 2rem; }
.row { display: flex; gap: .5rem; }
input { flex: 1; background: var(--surface-2); border: 1px solid var(--surface-5); border-radius: 10px; padding: .6rem .8rem; color: var(--color-contrast); }
button { background: var(--color-brand); color: var(--color-accent-contrast, #fff); border: 0; border-radius: 10px; padding: .55rem 1rem; font-weight: 600; cursor: pointer; }
button:disabled { opacity: .5; }
button.ghost { background: var(--surface-4); color: var(--color-base); }
.person { display: flex; align-items: center; gap: .6rem; padding: .5rem 0; border-top: 1px solid var(--surface-4); }
.person:first-of-type { border-top: 0; }
.person span { flex: 1; color: var(--color-contrast); }
.dot { width: 9px; height: 9px; border-radius: 50%; background: var(--surface-5); }
.dot.on { background: var(--color-brand); box-shadow: 0 0 8px var(--color-brand); }
.me { display: flex; align-items: center; gap: .5rem; }
.me img { width: 28px; height: 28px; border-radius: 6px; }
.muted { color: var(--color-secondary); }
.err { color: var(--color-red); margin: 0; }
</style>
