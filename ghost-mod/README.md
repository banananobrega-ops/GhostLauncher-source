# Ghost Launcher mod (Fabric 1.21.11)

Mod client-side que:

- mostra **a tua capa equipada** no launcher em qualquer server (troca a capa do teu jogador);
- mostra a capa de **outros jogadores do Ghost Launcher** que aparecerem perto de ti;
- poe a **logo do Ghost ao lado esquerdo do nome** (por cima da cabeca e no Tab) de quem usa o client.

So funciona para quem tem o mod, ou seja, quem joga pelo Ghost Launcher. Quem joga sem o client nao ve nada disto.

## Novo: menu do mod, amigos e chat

- **Right Shift** abre/fecha o menu do Ghost Launcher (`GhostMenuScreen`), com 3 separadores:
  - **Mods**: liga/desliga capas, badges, amigos no HUD e o pop-up de chat (guarda em `config/ghostclient.json`).
  - **Amigos**: pedir/aceitar/recusar amigos e falar com eles em tempo quase-real (poll a cada ~2s).
  - **Definicoes**: info basica + ultimo erro de rede, se houver.
- O **menu de ESC (pausa)** ganha um banner "GHOST CLIENT" e um botao de atalho no canto superior direito
  para abrir o menu do mod, sem mexer nos botoes originais (Opcoes, Sair, etc. continuam iguais).
- Os amigos/chat falam com os mesmos endpoints publicos do backend (`FriendsClient.java`), identificados
  por UUID (sem login/token, tal como o lookup de capas). Os endpoints ainda tem de ser criados no
  Lovable -- ver `LOVABLE_PROMPT_FRIENDS.md`.

## Como funciona

1. O launcher copia `ghost-client-mod.jar` para `mods/` das instancias **Fabric 1.21.11** antes de arrancar
   (`src-tauri/src/commands/ghost_mod.rs`).
2. O mod pergunta ao backend (`POST /api/public/gc/api/players/lookup`) quais dos jogadores que ve
   usam o Ghost Launcher e qual a capa ativa de cada um. Ver `LOVABLE_PROMPT.md`.
3. Descarrega a capa (PNG), regista-a como textura e troca a capa do jogador via Mixin.
4. A logo e um glifo de fonte (`assets/ghostclient/font/badge.json` + `textures/font/badge.png`)
   posto antes do nome. Para mudar a logo, substitui `badge.png` (PNG quadrado, ex. 32x32).

Nao precisa do Fabric API, so do Fabric Loader.

## Compilar

Pelo GitHub: **Actions -> Build Ghost Launcher mod -> Run workflow**, e descarrega o artefacto `ghost-client-mod`.
Os workflows `ci.yml` e `release.yml` compilam o mod sozinhos antes do launcher.

Local (precisa de Java 21 e Gradle 9.2+):

```
cd mod
gradle build
mkdir -p ../src-tauri/resources
cp build/libs/ghostclient-1.0.0.jar ../src-tauri/resources/ghost-client-mod.jar
```

Se o build reclamar de versoes, confirma os 4 valores de `gradle.properties` em https://fabricmc.net/develop (1.21.11).

## Testar sem o launcher

Poe o jar em `mods/` de um Minecraft 1.21.11 com Fabric Loader. No log tem de aparecer `Ghost Launcher mod ativo`.
