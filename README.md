# 🚛 Driver Shift Tracker (Трекер смен водителя)

**Android-приложение для учёта рабочих смен водителей-международников** с контролем соответствия регламенту ЕС № 561/2006.

> _An Android app for tracking driver shifts and monitoring EU Regulation 561/2006 compliance._

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/minSdk-24-brightgreen)](https://apilevels.com)
[![License](https://img.shields.io/badge/License-Private-lightgrey)]()

---

## 📋 Описание

Driver Shift Tracker помогает водителям-международникам вести ежедневный журнал рабочих смен, контролировать время вождения и отдыха, а также формировать отчёты для диспетчера. Приложение автоматически проверяет соблюдение основных норм регламента ЕС № 561/2006 и предупреждает о возможных нарушениях.

## ✨ Возможности

### 📊 Дашборд
- Время начала следующей смены с учётом **обычного** (11 ч) и **сокращённого** (9 ч) отдыха — отображается разными цветами
- Недельная сводка: общее время работы, вождения, расходы
- Визуальные индикаторы превышения лимитов (красный/зелёный)

### 📝 Журнал смен
- Добавление, редактирование и удаление записей смен
- **Поиск по дате** и **фильтрация по диапазону дат**
- Автосохранение черновика при закрытии формы
- Автозаполнение на основе предыдущей смены

### 📈 Отчёты
- Генерация текстовых отчётов за произвольный период
- Копирование отчёта в буфер обмена для отправки диспетчеру
- Понедельная разбивка с суммами часов и расходов

### ⚙️ Настройки
- **Профиль**: изменение имени водителя
- **Язык**: русский 🇷🇺 / английский 🇬🇧
- **Режим работы**:
  - _Расширенный_ — ввод времени обычной смены + тахографа
  - _Сокращённый_ — только время по тахографу
- **Нормативные документы**: справочник регламента ЕС № 561/2006
- **О программе**: версия и номер сборки

### ✅ Контроль соблюдения регламента ЕС
| Правило | Ограничение |
|---------|------------|
| Максимальная смена по тахографу | ≤ 13 часов (не более 3 раз/нед.) |
| Максимальное вождение в день | ≤ 9 часов (продление до 10 ч не более 2 раз/нед.) |
| Сокращённый ежедневный отдых | < 11 часов (не более 3 раз/нед.) |

---

## 🏗️ Архитектура

```
app/src/main/java/com/example/
├── MainActivity.kt          # Единственное Activity, весь UI на Jetpack Compose
├── data/
│   ├── Database.kt          # Room БД: DriverEntity, ShiftEntity, DAO
│   └── Repository.kt        # Репозиторий данных
├── domain/
│   └── ComplianceCalculator.kt  # Бизнес-логика проверки регламента
└── ui/
    ├── MainViewModel.kt     # MVVM ViewModel, управление состоянием
    └── Localization.kt      # Словарь переводов (RU/EN)
```

### Стек технологий

| Компонент | Технология |
|-----------|-----------|
| Язык | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Архитектура | MVVM (ViewModel + StateFlow) |
| База данных | Room (SQLite) |
| Асинхронность | Kotlin Coroutines + Flow |
| Сеть | Retrofit + OkHttp + Moshi |
| Сборка | Gradle (Kotlin DSL) |
| Тесты | JUnit + Robolectric + Roborazzi |

---

## 🚀 Сборка и запуск

### Требования
- **Android Studio** Ladybug (2024.2) или новее
- **JDK 11+** (встроен в Android Studio)
- **Android SDK** с compileSdk 36

### Шаги

1. **Клонировать репозиторий**
   ```bash
   git clone https://github.com/KindFest/driver-shift-tracker.git
   cd driver-shift-tracker
   ```

2. **Открыть в Android Studio**
   - File → Open → выбрать папку проекта
   - Дождаться синхронизации Gradle

3. **Запустить**
   - Выбрать устройство или эмулятор
   - Нажать ▶️ Run

### Сборка APK из командной строки

```bash
# Debug APK
./gradlew assembleDebug

# APK будет в: app/build/outputs/apk/debug/app-debug.apk
```

### Переменные окружения (для release-сборки)

```bash
KEYSTORE_PATH=/path/to/my-upload-key.jks
STORE_PASSWORD=your_store_password
KEY_PASSWORD=your_key_password
```

---

## 📱 Совместимость

- **Минимальная версия Android**: 7.0 (API 24)
- **Целевая версия Android**: 14 (API 36)
- **Поддерживаемые языки**: Русский, English

---

## 🗺️ Планы развития

- [ ] Ограничение 56 часов вождения за неделю
- [ ] Ограничение 90 часов вождения за 2 недели (скользящие)
- [ ] Контроль перерыва 45 мин после 4.5 часов вождения
- [ ] Еженедельный отдых ≥ 45 часов (сокращённый ≥ 24 ч)
- [ ] Максимум 6 суточных периодов вождения между еженедельными отдыхами
- [ ] Экспорт отчётов в PDF
- [ ] Уведомления о приближении к лимитам
- [ ] Резервное копирование данных в облако

---

## 📄 Лицензия

Данный проект является частным (private). Все права защищены.

---

<p align="center">
  <sub>Сделано с ❤️ для водителей-международников</sub>
</p>
