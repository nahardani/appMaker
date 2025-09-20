# AppMaker — Stage 1

Features (مرحله ۱):
- صفحهٔ ورود (نام کاربری `admin` / رمز `admin123`) — فقط دمویی.
- لیست پروژه‌ها + فرم ساخت پروژهٔ جدید (نام پروژه، نام شرکت).
- ذخیرهٔ پروژه‌ها در MongoDB (collection: `projects`).
- ویزارد گام ۲: انتخاب نسخهٔ جاوا (۸/۱۱/۱۷/۲۱) — برای «پروژهٔ تولیدی» در آینده.
- ویزارد گام ۳: انتخاب پکیج‌های استاندارد + افزودن پکیج‌های دلخواه.

## اجرا
1) MongoDB را اجرا کن (مثلاً با Docker):
   ```bash
   docker run -d --name mongo -p 27017:27017 mongo:6
   ```
   یا متغیر محیطی `MONGO_URI` را روی آدرس دلخواه تنظیم کن.

2) بیلد و اجرا:
   ```bash
   mvn clean package
   java -jar target/appmaker-stage1-0.0.1-SNAPSHOT.war
   ```
   (به‌صورت WAR هم قابل استقرار روی Tomcat است.)

3) ورود:
   - آدرس: http://localhost:8080
   - کاربری/رمز: `admin` / `admin123`

> توجه: خود این برنامه بر پایه Spring Boot 3.3.x است و **Java 17+** می‌خواهد.
> انتخاب نسخهٔ جاوا در ویزارد برای پروژهٔ خروجی شماست (در مراحل بعدی).

## ساختار مهم
- `Project` (مدل Mongo) شامل: `projectName`, `companyName`, `createdAt`, `javaVersion`, `packages`
- مسیرها:
  - `/login` (فرم ورود)
  - `/projects` (لیست/ایجاد پروژه)
  - `/wizard/{id}/java` (گام ۲)
  - `/wizard/{id}/packages` (گام ۳)
