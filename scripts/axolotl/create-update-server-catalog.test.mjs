import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'

const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'axolotl-update-catalog-'))
const releasePath = path.join(directory, 'release.json')
const output = path.join(directory, 'catalog.json')
const files = [
	'Ghost.Launcher-1.9.5-beta.1-1.aarch64.rpm',
	'Ghost.Launcher-1.9.5-beta.1-1.aarch64.rpm.sig',
	'Ghost.Launcher-1.9.5-beta.1-1.x86_64.rpm',
	'Ghost.Launcher-1.9.5-beta.1-1.x86_64.rpm.sig',
	'Ghost.Launcher_1.9.5-beta.1_aarch64.AppImage',
	'Ghost.Launcher_1.9.5-beta.1_aarch64.AppImage.sig',
	'Ghost.Launcher_1.9.5-beta.1_aarch64.AppImage.tar.gz',
	'Ghost.Launcher_1.9.5-beta.1_aarch64.AppImage.tar.gz.sig',
	'Ghost.Launcher_1.9.5-beta.1_amd64.AppImage',
	'Ghost.Launcher_1.9.5-beta.1_amd64.AppImage.sig',
	'Ghost.Launcher_1.9.5-beta.1_amd64.AppImage.tar.gz',
	'Ghost.Launcher_1.9.5-beta.1_amd64.AppImage.tar.gz.sig',
	'Ghost.Launcher_1.9.5-beta.1_amd64.deb',
	'Ghost.Launcher_1.9.5-beta.1_amd64.deb.sig',
	'Ghost.Launcher_1.9.5-beta.1_arm64.deb',
	'Ghost.Launcher_1.9.5-beta.1_arm64.deb.sig',
	'Ghost.Launcher_1.9.5-beta.1_universal.dmg',
	'Ghost.Launcher_1.9.5-beta.1_x64-setup.exe',
	'Ghost.Launcher_1.9.5-beta.1_x64-setup.exe.sig',
	'Ghost.Launcher_1.9.5-beta.1_x64-setup.nsis.zip',
	'Ghost.Launcher_1.9.5-beta.1_x64-setup.nsis.zip.sig',
	'Ghost.Launcher_universal.app.tar.gz',
	'Ghost.Launcher_universal.app.tar.gz.sig',
	'Ghost_Client_1.9.5-beta.1_x64_modern-setup.exe',
	'Ghost_Client_1.9.5-beta.1_x64_modern-setup.exe.sig',
	'Ghost_Client_1.9.5-beta.1_x64_nsis-setup.exe',
	'Ghost_Client_1.9.5-beta.1_x64_nsis-setup.exe.sig',
	'Ghost_Client_1.9.5-beta.1_x64_portable.zip',
]

try {
	fs.writeFileSync(
		releasePath,
		JSON.stringify({
			assets: files.map((name, index) => ({
				name,
				size: index + 1,
				digest: `sha256:${crypto.createHash('sha256').update(name).digest('hex')}`,
				browser_download_url: `https://github.com/Mystic-Stars/Ghost/releases/download/v1.9.5-beta.1/${name}`,
			})),
		}),
	)
	const result = spawnSync(
		process.execPath,
		['scripts/axolotl/create-update-server-catalog.mjs', releasePath, 'v1.9.5-beta.1', output],
		{ cwd: path.resolve(import.meta.dirname, '..', '..'), encoding: 'utf8' },
	)
	assert.equal(result.status, 0, result.stderr)
	const catalog = JSON.parse(fs.readFileSync(output, 'utf8'))
	assert.equal(catalog.version, '1.9.5-beta.1')
	assert.equal(catalog.files.length, files.length)
	assert.deepEqual(
		catalog.artifacts.find((artifact) => artifact.filename.endsWith('_amd64.AppImage.tar.gz'))
			.targetPlatforms,
		['linux-x86_64'],
	)
	assert.equal(
		catalog.artifacts.find((artifact) => artifact.filename.endsWith('modern-setup.exe')).variant,
		'modern',
	)
	assert.equal(
		catalog.artifacts.find((artifact) => artifact.filename.endsWith('portable.zip')).kind,
		'portable',
	)
	assert.deepEqual(
		catalog.artifacts.find((artifact) => artifact.filename.endsWith('_universal.app.tar.gz'))
			.targetPlatforms,
		['darwin-aarch64', 'darwin-x86_64'],
	)
} finally {
	fs.rmSync(directory, { recursive: true, force: true })
}
