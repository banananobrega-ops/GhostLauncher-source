# Discord Rich Presence - Setup em 2 Minutos

## O Problema

O launcher está configurado mas usa um ID falso (`1234567890123456789`). Discord precisa de um ID real de uma aplicação que TU crias. É igual ao Roblox, Spotify, etc - todos têm uma aplicação Discord.

## Setup Rápido (2 minutos)

### Passo 1: Criar Aplicação Discord
1. Vai para: https://discord.com/developers/applications
2. Login com a tua conta Discord
3. Clica em **"New Application"** (botão azul no canto superior direito)
4. Nome: **Ghost Launcher**
5. Clica **"Create"**

### Passo 2: Copiar o Application ID
1. Na página que abre, vês **"APPLICATION ID"** logo abaixo do nome
2. Clica no botão **"Copy"** ao lado
3. Guarda esse número - é tipo: `1234567890123456789` mas o teu próprio

### Passo 3: Upload do Icon (Opcional mas recomendado)
1. No menu esquerdo, vai em **"Rich Presence"** → **"Art Assets"**
2. Clica **"Add Image(s)"**
3. Upload da logo do Ghost Launcher (PNG ou JPG)
4. Nome da imagem: **ghost** (exatamente assim, minúsculas)
5. Clica **"Save Changes"**

### Passo 4: Configurar no Launcher

Opção A - Windows (PowerShell):
```powershell
# Substituir YOUR_APP_ID_HERE pelo ID que copiaste
$env:GHOST_DISCORD_CLIENT_ID="YOUR_APP_ID_HERE"
```

Opção B - Editar ficheiros (RECOMENDADO):

1. Abre: `packages/app-lib/.env.local`
2. Encontra: `GHOST_DISCORD_CLIENT_ID=1234567890123456789`
3. Substitui `1234567890123456789` pelo teu ID real
4. Guarda o ficheiro

5. Abre: `packages/app-lib/.env.prod`
6. Faz o mesmo - substitui pelo teu ID
7. Guarda o ficheiro

### Passo 5: Rebuild do Launcher
```bash
cd C:\Users\Banana\Downloads\GhostLauncher-source\GhostClient
npm run build
```

## Testar

1. Abre o Ghost Launcher
2. Abre o Discord
3. No teu perfil deve aparecer: **"Playing Ghost Launcher"**

## Troubleshooting

**Não aparece nada no Discord:**
- Verifica se o Application ID está correto nos ficheiros .env
- Rebuilda o launcher depois de mudar os ficheiros
- Reinicia o Discord
- Verifica nas configurações do Discord: User Settings → Activity Privacy → "Display current activity as a status message" está ATIVO

**Aparece erro no launcher:**
- O ID está errado
- Copia novamente da página do Discord Developer Portal

**A imagem não aparece:**
- Verifica se nomeaste a imagem como **"ghost"** (minúsculas)
- Espera 5-10 minutos - Discord demora a processar imagens novas

## Como Funciona

1. Quando o launcher abre, conecta ao Discord no teu PC
2. Envia para o Discord: "Estou a usar a aplicação com ID X"
3. Discord vai ver que aplicação é (Ghost Launcher)
4. Mostra no teu perfil: "Playing Ghost Launcher"

É exatamente como Roblox, Spotify, VS Code fazem. Todos têm um Application ID Discord.

## Porquê Preciso Criar a Aplicação?

O Discord não deixa usar IDs aleatórios. Cada aplicação/jogo precisa:
- Ser criada no Discord Developer Portal
- Ter um ID único
- Pertencer a alguém (tu, neste caso)

É grátis, leva 2 minutos, e funciona para sempre.
