# Lovable: endpoints de amigos + chat (para o mod in-game)

Cola isto no Lovable (depois do endpoint de lookup ja existente) e carrega em **Publish**:

```
Adiciona estes endpoints PUBLICOS (sem login, tal como /api/public/gc/api/players/lookup)
na mesma API. Tudo identificado por mc_uuid (uuid minecraft sem tracos), o mesmo campo
que o lookup ja usa. Cria as tabelas que precisares (friendships, friend_requests,
presence, chat_messages).

1) POST /api/public/gc/api/presence/ping
   Body: { "uuid": "<uuid sem tracos>", "username": "<nome atual>" }
   Marca este uuid como "online agora" (guarda username + last_seen = now()).
   Usa isto para o campo "online"/"ghost" na resposta de /friends/list: online = true se
   last_seen < 40 segundos atras.
   200 { "ok": true }

2) POST /api/public/gc/api/friends/request
   Body: { "uuid": "<uuid de quem pede>", "username": "<nome de quem pede>", "target_username": "<nome do alvo>" }
   Procura um utilizador pelo username (o mesmo username que o launcher guarda). Se nao existir -> 404.
   Se ja forem amigos -> 200 sem duplicar nada.
   Se ja houver um pedido do alvo para ti -> aceita automaticamente (cria a amizade) em vez de duplicar.
   Caso contrario cria um pedido pendente de uuid -> target_uuid.
   200 { "ok": true }

3) POST /api/public/gc/api/friends/accept
   Body: { "uuid": "<quem aceita>", "from_uuid": "<quem pediu>" }
   Confirma que existe um pedido pendente from_uuid -> uuid, cria a amizade (bidirecional)
   e apaga o pedido. 200 { "ok": true }. 404 se nao houver pedido.

4) POST /api/public/gc/api/friends/decline
   Body: { "uuid": "<quem recusa>", "from_uuid": "<quem pediu>" }
   Apaga o pedido pendente. 200 { "ok": true }.

5) POST /api/public/gc/api/friends/remove
   Body: { "uuid": "<quem remove>", "friend_uuid": "<amigo a remover>" }
   Remove a amizade nos dois sentidos. 200 { "ok": true }.

6) GET /api/public/gc/api/friends/list?uuid=<uuid sem tracos>
   Responde:
   {
     "friends": [
       { "uuid": "<sem tracos>", "username": "...", "online": true|false, "ghost": true, "lastSeen": <epoch ms ou null> }
     ],
     "incoming": [ { "uuid": "<sem tracos>", "username": "..." } ],
     "outgoing": [ { "uuid": "<sem tracos>", "username": "..." } ]
   }
   "friends" = lista de amizades confirmadas deste uuid (com o presence mais recente).
   "incoming" = pedidos que outros enviaram para este uuid.
   "outgoing" = pedidos que este uuid enviou e ainda estao pendentes.

7) POST /api/public/gc/api/chat/send
   Body: { "from_uuid": "<sem tracos>", "to_uuid": "<sem tracos>", "message": "<texto, max 500 chars>" }
   So permite enviar se from_uuid e to_uuid forem amigos (senao 403).
   Guarda a mensagem com um id unico e timestamp (ts em epoch ms).
   200 { "id": "...", "ts": 1234567890 }

8) GET /api/public/gc/api/chat/poll?uuid=<sem tracos>&since=<epoch ms>
   Responde com todas as mensagens onde from_uuid = uuid OU to_uuid = uuid, com ts > since,
   ordenadas por ts crescente, no maximo 200 de cada vez:
   { "messages": [ { "id": "...", "from": "<sem tracos>", "to": "<sem tracos>", "message": "...", "ts": 1234567890 } ] }
   Isto e chamado a cada ~2 segundos pelo mod (e um polling curto, nao websocket), por isso
   tem de ser rapido -- indice em (from_uuid, ts) e (to_uuid, ts).

Regras gerais (iguais ao endpoint de lookup):
- NAO devolvas email, tokens nem ids internos -- so uuid/username/mensagens.
- Limite de 60 pedidos por minuto por IP em cada endpoint.
- Nao ha token de sessao a validar quem e quem (tal como o lookup): confias no uuid enviado
  pelo mod, exatamente como ja fazes no /players/lookup. Isto e suficiente para amigos/chat
  informal dentro do jogo; nao guardes aqui nada sensivel.
```

## Testar

```
curl -X POST https://mc-social-core.lovable.app/api/public/gc/api/presence/ping \
  -H "content-type: application/json" \
  -d '{"uuid":"<o teu uuid sem tracos>","username":"<o teu nome>"}'

curl "https://mc-social-core.lovable.app/api/public/gc/api/friends/list?uuid=<o teu uuid sem tracos>"
```

A segunda tem de devolver `{"friends":[],"incoming":[],"outgoing":[]}` (lista vazia) se ainda nao tiveres amigos.
