# Ghost Launcher - Guia de Build Completo

## ❌ Problema: Node.js não está instalado

O build falhou porque **Node.js** não está instalado no teu PC. Precisas dele para compilar o launcher.

## 📥 Instalar Node.js (5 minutos)

### Opção 1: Download Direto (RECOMENDADO)
1. Vai a: https://nodejs.org/
2. Download do **LTS** (versão recomendada, tipo 20.x ou 22.x)
3. Instala o ficheiro `.msi`
4. Reinicia o PowerShell/CMD depois de instalar
5. Testa: `node --version` e `npm --version`

### Opção 2: Chocolatey (se tiveres)
```powershell
choco install nodejs
```

### Opção 3: Winget (Windows 11)
```powershell
winget install OpenJS.NodeJS
```

## 🔨 Depois de Instalar Node.js

### 1. Build do Launcher
```bash
cd C:\Users\Banana\Downloads\GhostLauncher-source\GhostClient
npm install
npm run build
```

### 2. Ou Modo Desenvolvimento (para testar)
```bash
npm run dev
```

## 🌍 Builds para Mac, Windows e Linux

**RESPOSTA**: Depende de onde fazes o build!

### Build no Windows (o teu caso)
Se fizeres `npm run build` no Windows, ele **só compila para Windows**.

Para compilar para todas as plataformas, precisas:

#### Opção A: GitHub Actions (AUTOMÁTICO) ✅
Se fizeres push para GitHub, podes configurar GitHub Actions para compilar automático para:
- ✅ Windows
- ✅ Mac
- ✅ Linux

#### Opção B: Manual em Cada OS
- Compilar no Windows → `.exe` para Windows
- Compilar no Mac → `.dmg` para Mac
- Compilar no Linux → `.AppImage` para Linux

#### Opção C: Cross-Compilation (Avançado)
Possível mas complicado. Não recomendado.

## 📦 Output do Build

Depois de `npm run build`, o executável fica em:
```
GhostClient/apps/app/src-tauri/target/release/
```

Procura por:
- **Windows**: `ghost-launcher.exe` ou similar
- **Mac**: `.app` bundle
- **Linux**: binário ou `.AppImage`

## 🚀 GitHub Actions Setup (Para Multi-Platform)

Se quiseres builds automáticos para todas as plataformas:

1. Cria ficheiro: `.github/workflows/build.yml`
2. Conteúdo:

```yaml
name: Build Ghost Launcher

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
    
    runs-on: ${{ matrix.os }}
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Setup Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '20'
    
    - name: Setup Rust
      uses: actions-rs/toolchain@v1
      with:
        toolchain: stable
    
    - name: Install dependencies
      working-directory: ./GhostClient
      run: npm install
    
    - name: Build
      working-directory: ./GhostClient
      run: npm run build
    
    - name: Upload artifacts
      uses: actions/upload-artifact@v3
      with:
        name: ghost-launcher-${{ matrix.os }}
        path: GhostClient/apps/app/src-tauri/target/release/
```

3. Push para GitHub
4. GitHub compila automático para Windows, Mac e Linux

## ⚡ Quick Start (TL;DR)

```bash
# 1. Instalar Node.js
# Download: https://nodejs.org/

# 2. Verificar instalação
node --version
npm --version

# 3. Build
cd C:\Users\Banana\Downloads\GhostLauncher-source\GhostClient
npm install
npm run build

# 4. Executável em:
# GhostClient/apps/app/src-tauri/target/release/
```

## ✅ Checklist

- [ ] Instalar Node.js
- [ ] Reiniciar PowerShell
- [ ] `npm install` no diretório GhostClient
- [ ] `npm run build`
- [ ] Testar o executável gerado
- [ ] Verificar Discord RPC funciona
- [ ] (Opcional) Setup GitHub Actions para multi-platform

## 🐛 Troubleshooting

**"npm não reconhecido"**: Node.js não instalado ou PATH não configurado
**Build falha com erro Rust**: Precisa instalar Rust também (`https://rustup.rs/`)
**Build demora muito**: Normal, primeira vez pode demorar 10-30 minutos
**Erro de permissões**: Corre PowerShell como Administrador

## 📱 Resumo da Tua Pergunta

> "o ghost launcher, ele ja esta a se construir para tipo mac windows e linux no meu pc ou eu tenho de fazer outra coisa?"

**RESPOSTA**: 
- ❌ No Windows, ele SÓ compila para Windows
- ✅ Para Mac e Linux, precisas compilar em cada OS OU usar GitHub Actions
- 🎯 Mais fácil: GitHub Actions compila para todos automaticamente
