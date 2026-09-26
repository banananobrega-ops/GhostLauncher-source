# Lovable: endpoint publico para o mod (capas + badge no jogo)

Cola isto no Lovable e depois carrega em **Publish**:

```
Cria um endpoint PUBLICO (sem login) POST /api/public/gc/api/players/lookup.

Body JSON: { "uuids": ["<uuid minecraft sem tracos>", ...] }
- maximo 100 uuids; aceita tambem uuids com tracos (normaliza para sem tracos); ignora os invalidos.
- 400 se "uuids" nao for um array ou tiver mais de 100.

Para cada uuid que exista nos utilizadores (campo mc_uuid, o mesmo que o login do launcher preenche), responde:
{ "players": { "<uuid sem tracos>": { "ghost": true, "capeUrl": "<url https publico do PNG da capa ATIVA>" | null } } }

Regras:
- uuid que nao existe nos utilizadores: nao aparece em "players".
- capeUrl = URL https direto e acessivel SEM autenticacao do PNG da capa ativa do utilizador
  (a que PUT /api/capes/active define). Sem capa ativa -> null.
  Se as capas estiverem num bucket privado do Supabase, torna o bucket das capas publico.
- NAO devolvas mais nada: nem username, nem email, nem tokens, nem ids internos.
- Header Cache-Control: public, max-age=30.
- Limite de 60 pedidos por minuto por IP.
```

## Testar

```
curl -X POST https://mc-social-core.lovable.app/api/public/gc/api/players/lookup \
  -H "content-type: application/json" \
  -d '{"uuids":["<o teu uuid sem tracos>"]}'
```

Tem de devolver `{"players":{"<uuid>":{"ghost":true,"capeUrl":"https://..."}}}`.
Se equipares uma capa no launcher, o `capeUrl` tem de mudar.
