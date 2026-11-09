# Маленькое письмо для тебя

Небольшое романтическое приложение для Windows. Открывается отдельным окном, не требует браузера и не подключается к интернету.

## Что внутри
- Анимированный фон с сердечками.
- Поля, где можно указать имя и подпись.
- Открывающееся письмо с тёплым посланием.
- Кнопка с дополнительными комплиментами-сюрпризами.

## Скачать
После успешной сборки открой [релиз Romantic Surprise](https://github.com/doniel1488-code/GreetMate/releases/tag/romantic-surprise-v1.0.0) и скачай `PismoDlyaTebya.exe`.

## Запуск
1. Скачай `PismoDlyaTebya.exe`.
2. Запусти двойным щелчком.
3. Введи имя и подпись, затем нажми «Открыть письмо».

Windows может показать предупреждение SmartScreen для приложения, которое не подписано издателем. Это не означает автоматически, что файл вредоносный: Windows просто не знает издателя. Скачивай файл только из этого релиза.

## Сборка самостоятельно
Нужен Python 3.12+:
```powershell
python -m pip install pyinstaller
python -m PyInstaller --noconfirm --clean --onefile --windowed --name PismoDlyaTebya romantic-surprise/main.py
```

Готовый файл появится в `dist/PismoDlyaTebya.exe`.

Исходный код: `romantic-surprise/main.py`.
