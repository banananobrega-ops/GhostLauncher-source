<script setup lang="ts">
import {
  CheckIcon,
  EditIcon,
  PlusIcon,
  SpinnerIcon,
  TrashIcon,
  UploadIcon,
} from '@modrinth/assets'
import {
  ButtonStyled,
  commonMessages,
  ConfirmModal,
  defineMessages,
  injectNotificationManager,
  useVIntl,
} from '@modrinth/ui'
import { arrayBufferToBase64 } from '@modrinth/utils'
import { invoke } from '@tauri-apps/api/core'
import type { DragDropEvent } from '@tauri-apps/api/webview'
import { getCurrentWebview } from '@tauri-apps/api/webview'
import { computed, inject, onMounted, onUnmounted, ref, type Ref } from 'vue'

import type AccountsCard from '@/components/ui/AccountsCard.vue'
import { useNetworkStatus } from '@/composables/useNetworkStatus'
import { get_default_user, users } from '@/helpers/auth'

const API = 'https://mc-social-core.lovable.app/api/public/gc'
const KEY = 'ghost.session'

type Cape = {
  id: string
  name: string
  texture: string
  is_active: boolean
  uploaded_at: string
}

const messages = defineMessages({
  capeManagerTitle: {
    id: 'app.capes.title',
    defaultMessage: 'Cape manager',
  },
  uploadCapeButton: {
    id: 'app.capes.upload-button',
    defaultMessage: 'Upload cape',
  },
  deleteCapeTitle: {
    id: 'app.capes.delete-modal.title',
    defaultMessage: 'Are you sure you want to delete this cape?',
  },
  deleteCapeDescription: {
    id: 'app.capes.delete-modal.description',
    defaultMessage: 'This will permanently delete the selected cape. This action cannot be undone.',
  },
  activeBadge: {
    id: 'app.capes.active-badge',
    defaultMessage: 'Active',
  },
  activateButton: {
    id: 'app.capes.activate-button',
    defaultMessage: 'Activate',
  },
  signInTitle: {
    id: 'app.capes.sign-in.title',
    defaultMessage: 'Please sign in',
  },
  signInDescription: {
    id: 'app.capes.sign-in.description',
    defaultMessage: 'Sign into your Ghost Client account to upload and manage your capes.',
  },
  signInButton: {
    id: 'app.capes.sign-in.button',
    defaultMessage: 'Sign in with Minecraft',
  },
  invalidFileTitle: {
    id: 'app.capes.invalid-file.title',
    defaultMessage: 'Invalid cape file',
  },
  invalidFileText: {
    id: 'app.capes.invalid-file.text',
    defaultMessage: 'Cape must be a PNG image with dimensions 64x32 or multiples (e.g., 128x64, 256x128).',
  },
  uploadSuccessTitle: {
    id: 'app.capes.upload-success.title',
    defaultMessage: 'Cape uploaded',
  },
  uploadSuccessText: {
    id: 'app.capes.upload-success.text',
    defaultMessage: 'Your cape has been uploaded successfully and is now visible to other Ghost Client users.',
  },
  noCapes: {
    id: 'app.capes.no-capes',
    defaultMessage: 'No capes uploaded yet. Upload your first cape!',
  },
})

const { formatMessage } = useVIntl()
const notifications = injectNotificationManager()
const { addNotification, handleError } = notifications
const { browserOffline, offline } = useNetworkStatus()

const accountsCard = inject('accountsCard') as Ref<typeof AccountsCard>
const token = ref<string | null>(localStorage.getItem(KEY))
const currentUser = ref<any>(null)
const currentUserId = ref<string | undefined>(undefined)
const capes = ref<Cape[]>([])
const isUploading = ref(false)
const isActivating = ref(false)
const isDraggingCapeFile = ref(false)
const uploadFileInput = ref<HTMLInputElement>()
const deleteCapeModal = ref()
const capeToDelete = ref<Cape | null>(null)

let userCheckInterval: number | null = null
let unlistenNativeDrop: (() => void) | null = null

const activeCape = computed(() => capes.value.find((c) => c.is_active))

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
  const payload =
    token.value && currentUser.value
      ? JSON.stringify({
          token: token.value,
          userId: currentUser.value.id,
          mcUuid: currentUser.value.mcUuid ?? currentUser.value.mc_uuid,
          mcUsername: currentUser.value.mcUsername ?? currentUser.value.name,
          apiBase: API,
        })
      : null
  try {
    await invoke('plugin:auth|ghost_write_session', { payload })
  } catch {
    /* mod sync optional */
  }
}

async function signIn() {
  isUploading.value = true
  try {
    const list: any[] = await users(false)
    const acc = list.find((u) => u.active) ?? list[0]
    if (!acc) throw new Error('Add a Minecraft account first (top left account menu).')
    const isOffline =
      acc.account_type === 'offline' || acc.account_type === 2 || acc.access_token === '0'
    const data = isOffline
      ? await api('/auth/offline', 'POST', { username: acc.profile?.name })
      : await api('/auth/minecraft', 'POST', { token: acc.access_token })
    token.value = data.session.token
    localStorage.setItem(KEY, token.value!)
    currentUser.value = data.user
    await writeModSession()
    await loadCapes()
  } catch (e: any) {
    handleError(e)
  } finally {
    isUploading.value = false
  }
}

function signOut() {
  token.value = null
  currentUser.value = null
  capes.value = []
  localStorage.removeItem(KEY)
  writeModSession()
}

async function loadCapes() {
  if (!token.value) return
  try {
    const data = await api('/capes')
    capes.value = data.capes ?? []
  } catch (e: any) {
    handleError(e)
  }
}

async function loadCurrentUser() {
  try {
    const defaultId = await get_default_user(offline.value)
    currentUserId.value = defaultId

    const allAccounts = await users(offline.value)
    const selectedAccount = allAccounts.find((acc) => acc.account_id === defaultId)
    currentUser.value = selectedAccount

    // Check if we have a stored session
    if (token.value) {
      const session = await api('/auth/get-session')
      if (session?.user) {
        currentUser.value = session.user
        await loadCapes()
      } else {
        signOut()
      }
    }
  } catch (e: any) {
    if (e.message !== 'Unauthorized') {
      handleError(e)
    }
    currentUser.value = undefined
    currentUserId.value = undefined
  }
}

function openUploadFileBrowser() {
  uploadFileInput.value?.click()
}

async function onUploadFileInputChange(e: Event) {
  const files = (e.target as HTMLInputElement).files
  const file = files?.[0]

  if (!file) return

  await processCapeFile(await file.arrayBuffer())

  if (uploadFileInput.value) {
    uploadFileInput.value.value = ''
  }
}

function isCapeImagePath(path: string) {
  return path.toLowerCase().endsWith('.png')
}

function isCapeFileDrag(event: DragEvent) {
  const items = Array.from(event.dataTransfer?.items ?? [])
  const files = Array.from(event.dataTransfer?.files ?? [])

  return (
    items.some((item) => item.kind === 'file' && item.type === 'image/png') ||
    files.some((file) => file.type === 'image/png' || isCapeImagePath(file.name))
  )
}

function onCapeFileDragOver(event: DragEvent) {
  if (!token.value || !isCapeFileDrag(event)) return
  event.preventDefault()
  isDraggingCapeFile.value = true
}

function onCapeFileDragLeave() {
  isDraggingCapeFile.value = false
}

async function onCapeFileDrop(event: DragEvent) {
  isDraggingCapeFile.value = false

  const file = Array.from(event.dataTransfer?.files ?? []).find(
    (file) => file.type === 'image/png' || isCapeImagePath(file.name),
  )

  if (!file) return
  await processCapeFile(await file.arrayBuffer())
}

async function setupNativeDropHandler() {
  try {
    unlistenNativeDrop = await getCurrentWebview().onDragDropEvent(
      (event: { payload: DragDropEvent }) => {
        const payload = event.payload
        if (payload.type !== 'drop' || !token.value) return

        const pngPath = (payload.paths as string[]).find((p: string) =>
          p.toLowerCase().endsWith('.png'),
        )
        if (!pngPath) return

        readDroppedCapeFile(pngPath)
      },
    )
  } catch (error) {
    console.warn('Failed to set up native drop handler on cape page', error)
  }
}

async function readDroppedCapeFile(path: string) {
  try {
    const data = await invoke<ArrayBuffer>('plugin:files|file_read_dragged_file', { path })
    await processCapeFile(new Uint8Array(data))
  } catch (error) {
    handleError(error as Error)
  }
}

async function processCapeFile(buffer: Uint8Array | ArrayBuffer) {
  if (!token.value) return

  isUploading.value = true
  try {
    // Validate image dimensions
    const base64 = arrayBufferToBase64(buffer)
    const img = new Image()

    await new Promise((resolve, reject) => {
      img.onload = resolve
      img.onerror = reject
      img.src = `data:image/png;base64,${base64}`
    })

    // Check if dimensions are valid (64x32 or multiples)
    const validWidth = img.width % 64 === 0 && img.width / 64 === img.height / 32
    const validHeight = img.height % 32 === 0

    if (!validWidth || !validHeight || img.width / img.height !== 2) {
      addNotification({
        type: 'error',
        title: formatMessage(messages.invalidFileTitle),
        text: formatMessage(messages.invalidFileText),
      })
      return
    }

    // Upload to API
    await api('/capes', 'POST', {
      texture: `data:image/png;base64,${base64}`,
      name: `Cape ${new Date().toLocaleDateString()}`,
    })

    await loadCapes()

    addNotification({
      type: 'success',
      title: formatMessage(messages.uploadSuccessTitle),
      text: formatMessage(messages.uploadSuccessText),
    })
  } catch (error) {
    handleError(error as Error)
  } finally {
    isUploading.value = false
  }
}

async function activateCape(cape: Cape) {
  if (isActivating.value) return

  isActivating.value = true
  try {
    await api(`/capes/${cape.id}/activate`, 'POST')
    await loadCapes()
  } catch (error) {
    handleError(error as Error)
  } finally {
    isActivating.value = false
  }
}

function confirmDeleteCape(cape: Cape) {
  capeToDelete.value = cape
  deleteCapeModal.value?.show()
}

async function deleteCape() {
  if (!capeToDelete.value) return

  try {
    await api(`/capes/${capeToDelete.value.id}`, 'DELETE')
    await loadCapes()
  } catch (error) {
    handleError(error as Error)
  } finally {
    capeToDelete.value = null
  }
}

async function checkUserChanges() {
  try {
    const defaultId = await get_default_user(offline.value)
    if (defaultId !== currentUserId.value) {
      await loadCurrentUser()
    }
  } catch (error) {
    if (currentUser.value && error instanceof Error) {
      handleError(error)
    }
  }
}

onMounted(() => {
  loadCurrentUser()
  userCheckInterval = window.setInterval(checkUserChanges, 250)
  setupNativeDropHandler()
})

onUnmounted(() => {
  if (userCheckInterval !== null) {
    window.clearInterval(userCheckInterval)
  }
  if (unlistenNativeDrop) {
    unlistenNativeDrop()
  }
})
</script>

<template>
  <input
    ref="uploadFileInput"
    type="file"
    accept="image/png"
    class="hidden"
    @change="onUploadFileInputChange"
  />
  <ConfirmModal
    ref="deleteCapeModal"
    :title="formatMessage(messages.deleteCapeTitle)"
    :description="formatMessage(messages.deleteCapeDescription)"
    :proceed-label="formatMessage(commonMessages.deleteLabel)"
    @proceed="deleteCape"
  />

  <div
    v-if="token && currentUser"
    data-onboarding-id="capes-page"
    class="cape-layout box-border min-h-full p-4"
  >
    <div class="flex flex-col gap-6">
      <div class="flex items-center justify-between">
        <h1 class="m-0 text-2xl font-bold">
          {{ formatMessage(messages.capeManagerTitle) }}
        </h1>
        <ButtonStyled color="brand">
          <button :disabled="isUploading" @click="openUploadFileBrowser">
            <SpinnerIcon v-if="isUploading" class="animate-spin" />
            <UploadIcon v-else />
            {{ formatMessage(messages.uploadCapeButton) }}
          </button>
        </ButtonStyled>
      </div>

      <div
        v-if="capes.length === 0"
        class="flex flex-col items-center justify-center gap-4 rounded-lg border-2 border-dashed border-surface-5 bg-surface-2 p-12 text-center"
        :class="{ 'border-brand': isDraggingCapeFile }"
        @dragenter="onCapeFileDragOver"
        @dragover="onCapeFileDragOver"
        @dragleave="onCapeFileDragLeave"
        @drop.prevent="onCapeFileDrop"
      >
        <UploadIcon class="size-16 text-secondary" />
        <div>
          <p class="m-0 text-lg font-semibold text-primary">
            {{ formatMessage(messages.noCapes) }}
          </p>
          <p class="m-0 mt-2 text-sm text-secondary">
            Drop a PNG file here or click the upload button
          </p>
          <p class="m-0 mt-1 text-xs text-secondary">
            Cape dimensions: 64x32 or multiples (128x64, 256x128, etc.)
          </p>
        </div>
      </div>

      <div
        v-else
        class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3"
      >
        <div
          v-for="cape in capes"
          :key="cape.id"
          class="relative flex flex-col overflow-hidden rounded-lg border border-surface-5 bg-surface-3 transition-shadow hover:shadow-lg"
        >
          <div
            class="flex aspect-[2/1] items-center justify-center bg-surface-2 p-4"
          >
            <img
              :src="cape.texture"
              :alt="cape.name"
              class="max-h-full max-w-full object-contain"
              style="image-rendering: pixelated"
            />
          </div>
          <div class="flex flex-col gap-2 p-4">
            <div class="flex items-start justify-between gap-2">
              <div class="flex-1">
                <h3 class="m-0 text-base font-semibold text-primary">
                  {{ cape.name }}
                </h3>
                <p class="m-0 mt-1 text-xs text-secondary">
                  {{ new Date(cape.uploaded_at).toLocaleDateString() }}
                </p>
              </div>
              <div
                v-if="cape.is_active"
                class="flex shrink-0 items-center gap-1.5 rounded-full bg-bg-blue px-2 py-1 text-xs font-semibold text-brand-blue"
              >
                <CheckIcon class="size-3" />
                {{ formatMessage(messages.activeBadge) }}
              </div>
            </div>
            <div class="flex gap-2">
              <button
                v-if="!cape.is_active"
                class="flex h-8 flex-1 cursor-pointer items-center justify-center gap-1.5 rounded-lg border-0 bg-brand px-3 text-sm font-semibold text-[rgba(0,0,0,0.9)] transition-[filter,transform] duration-200 enabled:hover:brightness-[--hover-brightness] enabled:active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
                :disabled="isActivating"
                @click="activateCape(cape)"
              >
                <SpinnerIcon v-if="isActivating" class="size-4 animate-spin" />
                <CheckIcon v-else class="size-4" />
                {{ formatMessage(messages.activateButton) }}
              </button>
              <button
                class="flex h-8 cursor-pointer items-center justify-center gap-1.5 rounded-lg border-0 bg-surface-4 px-3 text-sm font-semibold text-contrast transition-[filter,transform] duration-200 enabled:hover:brightness-[--hover-brightness] enabled:active:scale-95"
                @click="confirmDeleteCape(cape)"
              >
                <TrashIcon class="size-4" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div
    v-else
    data-onboarding-id="capes-page"
    class="box-border flex min-h-full items-center justify-center pt-[25%]"
  >
    <div
      class="relative mx-auto flex w-full max-w-xl flex-col gap-5 rounded-lg bg-bg-raised p-7 shadow-lg"
    >
      <div
        class="absolute top-0 left-0 h-[1px] w-full bg-gradient-to-r from-transparent via-green-500 to-transparent opacity-40"
        style="
          background: linear-gradient(
            to right,
            transparent 2rem,
            var(--color-green) calc(100% - 13rem),
            var(--color-green) calc(100% - 5rem),
            transparent calc(100% - 2rem)
          );
        "
      ></div>

      <div class="flex flex-col gap-5">
        <h1 class="m-0 text-3xl font-extrabold">
          {{ formatMessage(messages.signInTitle) }}
        </h1>
        <p class="m-0 text-lg">
          {{ formatMessage(messages.signInDescription) }}
        </p>
        <ButtonStyled v-if="!offline && !browserOffline" color="brand">
          <button :disabled="isUploading" @click="signIn">
            <SpinnerIcon v-if="isUploading" class="animate-spin" />
            {{ formatMessage(messages.signInButton) }}
          </button>
        </ButtonStyled>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.cape-layout {
  max-width: 1200px;
  margin: 0 auto;
}
</style>
