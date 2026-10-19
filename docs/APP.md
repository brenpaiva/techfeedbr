# Aplicativo Android — TechFeed BR

## Funcionalidades

O TechFeed BR é um aplicativo nativo Android de notícias de tecnologia. Oferece onboarding, feed paginado, busca, filtros de assunto/idioma/leitura, ordenação por data ou relevância personalizada, favoritos, “ler depois”, abertura da publicação original, preferências, tema escuro e status das fontes. O menu lateral não oferece filtro por fonte. O feed começa em português; o usuário pode selecionar todos os idiomas.

A interface editorial usa azul e verde, miniaturas reais quando disponíveis e 20 composições tecnológicas locais como alternativa visual. A área de busca se contrai durante a rolagem. Os layouts atendem celular e tablet.

## Arquitetura e dados locais

- **UI:** Kotlin, Jetpack Compose, Material 3 e Navigation Compose.
- **Rede:** Retrofit e Gson consomem a [API do TechFeed BR](API.md).
- **Persistência:** Room guarda notícias reais, favoritos, “ler depois”, leitura e eventos da avaliação; DataStore guarda preferências.
- **Imagens:** Coil tenta a miniatura da fonte. A arte local substitui imagens ausentes ou indisponíveis; ela não representa notícia simulada.

O app usa `nextCursor` para carregar mais páginas. Busca, filtros e ordenação consultam o backend com os mesmos parâmetros durante a paginação. Se a rede falhar, notícias reais já salvas permanecem visíveis com indicação de desatualização. Somente quando não houver notícia real recebida nem salva aparecem exemplos locais identificados como **EXEMPLO**. Eles não são enviados à API nem entram na avaliação.

Favoritos e estados de leitura persistem ao reiniciar. O Android Backup está desabilitado para manter os dados locais sob controle do usuário.

## Avaliação no APK

O participante informa códigos de sessão e tarefa e aceita a coleta antes de iniciar. O app registra eventos localmente e permite exportá-los em CSV/JSON ou excluí-los. O texto digitado na busca não é registrado. A avaliação da **versão web** usa endpoints próprios da API e retenção de 30 dias, descritos em [API.md](API.md); ela não altera a avaliação local do APK.

## Executar e testar

Requisitos: JDK 17, Android Studio com Android SDK 37 e emulador ou dispositivo Android 8.0+ (`minSdk 26`). Inicie antes a API local conforme [API.md](API.md).

```powershell
cd mobile
.\gradlew.bat testDebugUnitTest assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

O APK debug sai em `mobile/app/build/outputs/apk/debug/`. No emulador, `10.0.2.2` aponta para o computador. Se o Docker Desktop não encaminhar a porta ao emulador, use `adb reverse tcp:8080 tcp:8080` e compile com `-PTECHFEED_API_BASE_URL=http://127.0.0.1:8080/`.

Os testes automatizados cobrem busca/paginação, fallback e persistência. A [spec](TECHFEED_SPEC.md) reúne evidências; ensaio em aparelho físico e roteiro completo com TalkBack ainda são verificações manuais pendentes.

## Gerar release

O release requer `mobile/signing.local.properties` com `storeFile`, `storePassword`, `keyAlias` e `keyPassword`, além do keystore correspondente. Ambos ficam fora do Git. Guarde a chave: a mesma é necessária para atualizar o aplicativo instalado.

```powershell
cd mobile
.\gradlew.bat assembleRelease -PTECHFEED_RELEASE_API_BASE_URL=https://api.brenopaiva.dev/techfeed/
```

O build exige HTTPS, verifica `/actuator/health` e exige que `/api/v1/feed` entregue ao menos uma notícia real. O APK sai em `mobile/app/build/outputs/apk/release/`. A versão para a banca é **0.4.0** (`versionCode 5`), disponível no [download público](https://api.brenopaiva.dev/static/techfeed/TechFeedBR-release.apk).

## Código principal

- `mobile/app/src/main/java/br/com/techfeedbr/ui/`: telas, tema e miniaturas.
- `mobile/app/src/main/java/br/com/techfeedbr/data/`: API, repositório e banco local.
- `mobile/app/src/main/java/br/com/techfeedbr/evaluation/`: avaliação Android.
- `mobile/app/src/test/` e `mobile/app/src/androidTest/`: testes JVM e instrumentados.
