# Foxium Browser for Android 🦊⚡

> **Движок**: Chromium (Blink & V8 via AndroidX WebKit)  
> **Интерфейс**: Firefox Fenix (Bottom Toolbar, Tab Tray, Dark Theme, Tracking Protection)  
> **CI/CD**: Автоматическая сборка APK через GitHub Actions

---

## 🚀 Особенности

- **Chromium Engine**: Аппаратное ускорение, современный рендеринг веб-страниц, поддержка новейших стандартов HTML5/CSS3/JavaScript.
- **Интерфейс Firefox**:
  - **Нижняя адресная строка**: Удобное управление одной рукой (Bottom Address Bar).
  - **Firefox Tab Tray**: Вкладки выезжают шторкой снизу в виде карточек.
  - **Приватный режим (Private Mode)**: Отдельный изолированный профиль с фиолетовым акцентом Firefox, очисткой кэша и cookie.
  - **Firefox Home**: Стартовая страница с быстрым доступом к популярным сервисам и переключателем поисковиков (DuckDuckGo, Google, Яндекс).
- **Улучшенная защита от отслеживания (Enhanced Tracking Protection)**: Встроенный блокировщик трекеров, аналитики и рекламных скриптов без сторонних тяжелых библиотек.
- **Поддержка версии для ПК (Desktop Mode)** и быстрый шеринг страниц.
- **Готов к роли браузера по умолчанию**: Зарегистрирован для перехвата ссылок `http://` и `https://`.

---

## 🛠 Автоматическая сборка в GitHub Actions

В репозитории уже настроен `.github/workflows/build-apk.yml`. 

### Как собрать APK:

1. **Создайте новый репозиторий на GitHub** (например, `foxium-browser`).
2. **Инициализируйте и запушьте код**:
   ```bash
   cd foxium-browser
   git init
   git add .
   git commit -m "feat: initial commit of Foxium Browser"
   git branch -M main
   git remote add origin https://github.com/<ВАШ_АККАУНТ>/foxium-browser.git
   git push -u origin main
   ```
3. **Скачайте готовый APK**:
   - Перейдите во вкладку **Actions** в вашем репозитории на GitHub.
   - Выберите последний запущенный workflow `Build Foxium Android APK`.
   - В разделе **Artifacts** скачайте архив `Foxium-Browser-APKs` с готовыми файлами:
     - `Foxium-Browser-Release.apk`
     - `Foxium-Browser-Debug.apk`
   - Передайте APK на телефон и установите!

4. **Автоматический релиз по тегу (Releases)**:
   При создании любого тега версии (например, `git tag v1.0.0 && git push origin v1.0.0`) GitHub Actions автоматически создаст **GitHub Release** и прикрепит готовый файл `Foxium-Browser-Release.apk`.

---

## 📁 Структура проекта

```
foxium-browser/
├── .github/workflows/
│   └── build-apk.yml               # CI/CD сборщик для GitHub Actions
├── app/
│   ├── build.gradle.kts            # Конфигурация Android, Compose и WebKit
│   └── src/main/
│       ├── AndroidManifest.xml     # Регистрация браузера в системе Android
│       ├── java/com/foxium/browser/
│       │   ├── MainActivity.kt     # Главный контроллер навигации и вкладок
│       │   ├── adblock/
│       │   │   └── TrackerBlocker.kt # Блокировщик трекеров (Enhanced Tracking Protection)
│       │   ├── data/
│       │   │   ├── TabModel.kt     # Модель вкладки (URL, title, private mode)
│       │   │   └── SearchEngine.kt # Поисковые движки (DuckDuckGo, Google, Yandex)
│       │   └── ui/
│       │       ├── theme/          # Цветовая палитра Firefox Dark & Private
│       │       └── components/
│       │           ├── BottomAddressBar.kt # Нижняя панель поиска Firefox
│       │           ├── TabTraySheet.kt     # Шторка управления вкладками
│       │           ├── HomeScreen.kt       # Стартовый экран Foxium Home
│       │           ├── BrowserMenuSheet.kt # Контекстное меню браузера
│       │           └── BrowserView.kt      # Интеграция Chromium WebView
```
