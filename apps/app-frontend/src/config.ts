const trimTrailingSlash = (url: string) => url.replace(/\/$/, '')

export const AxolotlBrandConfig = Object.freeze({
	productName: 'Ghost Launcher',
	shortProductName: 'Ghost',
	website: 'https://mc-social-core.lovable.app/',
	repositoryUrl: 'https://github.com/Mystic-Stars/Ghost',
	supportUrl: 'https://github.com/Mystic-Stars/Ghost/issues',
	qqGroupNumber: '737601250',
	qqChannelUrl: 'https://pd.qq.com/s/9nfp5rlz0',
	sponsorUrl: 'https://afdian.com/a/Mystic-Stars',
	bundleIdentifier: 'app.ghostclient.launcher',
	deepLinkScheme: 'ghostclient',
	userAgent: (version: string, os: string) => `ghostclient/launcher/${version} (${os})`,
	capabilities: Object.freeze({
		publicModrinthApi: true,
		privateModrinthServices: false,
		ghsTelemetry: false,
	}),
})

const siteUrl = trimTrailingSlash(import.meta.env.MODRINTH_URL || 'https://modrinth.com')
const officialLabrinthBaseUrl = trimTrailingSlash(
	import.meta.env.MODRINTH_API_BASE_URL || 'https://api.modrinth.com',
)
type DownloadSourceMode = 'auto' | 'official_only' | 'mirror_preferred' | 'official_preferred'

// The Modrinth API always uses the official source; Modrinth download mirror
// routing is handled by the Rust download layer.
export function setModrinthSourceMode(_sourceMode: DownloadSourceMode) {}

export function setModrinthMirrorEnabled(_enabled: boolean) {}

export function getOfficialLabrinthBaseUrl() {
	return officialLabrinthBaseUrl
}

export function getLabrinthBaseUrl() {
	return officialLabrinthBaseUrl
}

export const config = {
	siteUrl,
	labrinthBaseUrl: getLabrinthBaseUrl,
}
