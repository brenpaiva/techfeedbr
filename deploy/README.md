# TechFeed BR em produção

## Endereços e artefatos

- API: `https://api.brenopaiva.dev/techfeed/`
- Saúde: `https://api.brenopaiva.dev/techfeed/actuator/health`
- Feed: `https://api.brenopaiva.dev/techfeed/api/v1/feed`
- APK release: `https://api.brenopaiva.dev/static/techfeed/TechFeedBR-release.apk`
- Versão do APK: `0.4.0` (`versionCode=5`)
- SHA-256 do APK: `126E437C64B598540B9162BA0F2114BFC067AEEB67B0A66A6F504E2DEB80A367`

## Servidor

O Docker Compose de produção fica em `/srv/techfeed-br/compose.yaml`, com a mesma configuração versionável de [compose.prod.yaml](compose.prod.yaml). O arquivo `/srv/techfeed-br/.env` contém a senha gerada para o PostgreSQL e não deve ser copiado para o repositório. A API não expõe porta no host; somente a rede interna do proxy Nginx alcança o contêiner. O PostgreSQL usa volume próprio e não possui porta pública.

A imagem atual é `techfeed-backend:2026-10-19-web-eval`; a configuração anterior foi preservada em `/srv/techfeed-br/deploy-backups/compose.pre-web-eval.yaml` e o APK anterior em `TechFeedBR-release-0.3.1.apk` no volume estático. Em 19/10/2026, o serviço HTTPS entregou notícias reais em português à versão web. TabNews e GNews estão desabilitados e ocultos do feed. A imagem foi montada a partir do JAR testado com [Dockerfile.runtime](Dockerfile.runtime), sem compilar Java no host de produção.

O Nginx existente tem a rota `/techfeed/` no modelo `/home/brenpaiva/portifolio/nginx/conf.d/default.https.conf`. Uma cópia do modelo anterior está ao lado dele, com sufixo `.pre-techfeed-20261009`. Uma cópia do arquivo ativo anterior está em `/srv/techfeed-br/nginx-before.conf`. A rota usa o DNS interno do Docker em tempo de requisição, permitindo que os outros sites iniciem mesmo se a API do TechFeed estiver indisponível.

### Demonstração web do TCC (19/10/2026)

- Site: `https://tcc.brenopaiva.dev`; o código web está em um repositório Git separado, com checkout de implantação em `/srv/techfeed-br-web`.
- O build estático ocupa `techfeed-web/` no volume `portifolio_static_volume`, já montado como somente leitura no Nginx. A configuração do novo site e o limite de requisições estão versionados no repositório web. O caminho antigo `/techfeed/api/v1/web-evaluations` foi fechado no domínio `api.brenopaiva.dev`; os endpoints de avaliação são encaminhados apenas por `tcc.brenopaiva.dev`.
- O Certbot existente emitiu o certificado do subdomínio, válido inicialmente até 17/01/2027. O timer `techfeed-tcc-nginx-reload.timer` recarrega o Nginx diariamente para aplicar renovações.
- A migração V3 adicionou sessões e eventos web ao PostgreSQL. Eventos só são enviados após aceite, autenticados por token de sessão, e expiram em 30 dias. Use `/srv/techfeed-br/export-web-evaluations.sh` via SSH para gerar CSV e JSON privados em `/srv/techfeed-br/evaluation-exports/` (permissões 700/600). O script correspondente está neste diretório de deploy.
- O JAR, a API, o feed real, a criação/consulta/exclusão de uma sessão de ensaio, o site principal e o APK público foram testados após a publicação. A sessão de ensaio foi excluída. A versão anterior da configuração do Nginx fica em `/srv/techfeed-br/deploy-backups/` e a imagem antiga permanece disponível para reversão.

O servidor tem pouca RAM. O Compose limita a API a 384 MiB e o banco a 192 MiB, e os logs dos dois serviços giram em arquivos de até 10 MiB. Foi adicionado swap de 512 MiB persistido em `/etc/fstab`. Os logs antigos do sistema foram reduzidos de cerca de 986 MiB para 218 MiB; `/etc/systemd/journald.conf.d/techfeed-disk-limit.conf` mantém o journal abaixo de 250 MiB e reserva 1 GiB livre. Após a limpeza, havia cerca de 1,4 GiB disponíveis no disco. Verificar espaço e memória antes de atualizações:

```sh
df -h /
free -h
cd /srv/techfeed-br && docker compose ps
docker stats --no-stream techfeed-br-api techfeed-br-db-1
```

## Operação e atualização

```sh
cd /srv/techfeed-br
docker compose up -d
docker compose logs --tail=100 api
```

GNews e TabNews foram desativados por solicitação do usuário. Mesmo que uma chave de GNews seja adicionada ao `.env`, a fonte permanece desabilitada até uma decisão explícita de reativação.

Uma nova versão do backend deve ser construída em ambiente de desenvolvimento, carregada no servidor como imagem Docker, receber nova tag em `compose.yaml` e ser iniciada com `docker compose up -d --no-deps api`. Testar health e feed por HTTPS antes de gerar outro release. Para gerar o APK assinado:

```powershell
cd mobile
.\gradlew.bat :app:assembleRelease -PTECHFEED_RELEASE_API_BASE_URL=https://api.brenopaiva.dev/techfeed/
```

O build exige `mobile/signing.local.properties` e o keystore local, ambos ignorados pelo Git. Guardar os dois arquivos em local seguro para permitir atualização do aplicativo instalado.

Depois de validar uma nova versão, copiar o APK para o servidor e publicá-lo em `/var/lib/docker/volumes/portifolio_static_volume/_data/techfeed/TechFeedBR-release.apk`, com diretório `755` e arquivo `644`. Atualizar também `TechFeedBR-release.apk.sha256` nesse diretório e os hashes documentados neste arquivo e na spec. O endereço público do APK é o indicado acima. A publicação atual foi conferida por HTTPS e pelo hash SHA-256.
