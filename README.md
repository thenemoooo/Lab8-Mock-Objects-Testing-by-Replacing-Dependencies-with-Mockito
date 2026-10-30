# Лаборатори №8 — Mock объектууд: Mockito-гоор хамаарлыг орлуулж тестлэх

**Оюутан:** Nyamod Nyamsuren · **Код:** B222270805 · F.CSA313 (2026)

`CourseRegistrationService` нь хоёр гадаад хамааралтай (`StudentRepository`, `EmailSender`).
Тэдгээрийг Mockito-гоор орлуулж, үйлчилгээний логикийг бодит сан, SMTP сервергүйгээр
нэгжийн түвшинд тестэлсэн.

## 1. Ажлын орчин

```text
$ java -version
java version "23.0.2" 2025-01-21
Java(TM) SE Runtime Environment (build 23.0.2+7-58)
Java HotSpot(TM) 64-Bit Server VM (build 23.0.2+7-58, mixed mode, sharing)

$ mvn -version
Apache Maven 3.9.12 (848fbb4bf2d427b72bdb2471c22fced7ebd9a7a1)
Maven home: /opt/homebrew/Cellar/maven/3.9.12/libexec
Java version: 25.0.2, vendor: Homebrew, runtime: /opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home
Default locale: en_MN, platform encoding: UTF-8
OS name: "mac os x", version: "26.5", arch: "aarch64", family: "mac"
```

Төсөл `maven.compiler.release=17`-оор компиллогддог тул Java 17 ба түүнээс дээш хувилбар дээр ажиллана.
JDK 21+ дээр гардаг `WARNING: A Java agent has been loaded dynamically (byte-buddy-agent ...)`
анхааруулга нь Mockito-гийн inline mock maker-ийн хэвийн анхааруулга — алдаа биш.

## 2. Төслийн бүтэц

```text
lab08-mockito/
├── pom.xml
├── README.md
├── .gitignore
├── src/main/java/mn/edu/must/sqat/
│   ├── Student.java                    # record — оюутны өгөгдөл
│   ├── StudentRepository.java          # интерфейс (сан), хэрэгжүүлэлтгүй
│   ├── EmailSender.java                # интерфейс (и-мэйл), хэрэгжүүлэлтгүй
│   └── CourseRegistrationService.java  # тестлэгдэх класс
├── src/test/java/mn/edu/must/sqat/
│   └── CourseRegistrationServiceTest.java   # Хэсэг A — 10 гүйцэтгэл
├── ai-review/
│   └── AiCourseRegistrationServiceTest.java # Сонголт 2 — AI-ийн тест (үндсэн suite-д ОРООГҮЙ)
└── results/
    ├── mvn-test.txt              # ногоон: Tests run: 10, Failures: 0
    ├── mvn-test-mutant-1.txt     # BUILD FAILURE
    ├── mvn-test-mutant-2.txt     # BUILD FAILURE
    ├── mvn-test-mutant-3.txt     # BUILD FAILURE
    └── ai-review/                # Сонголт 2-ын AI тестийн гаралтууд
```

## 3. Ажиллуулах

```bash
mvn test
```

Хүлээгдэх үр дүн — **Tests run: 10, Failures: 0, Errors: 0, Skipped: 0**, BUILD SUCCESS.
Энэ тоо нь `results/mvn-test.txt` файлын сүүлийн `Tests run:` мөртэй яг таарна:
7 энгийн тест (A1–A7) + A8-ийн параметржүүлсэн 3 мөр = **10**.

## 4. Хэсэг A — найман тест

| № | Тестийн метод | Гол хэрэгсэл |
|---|---|---|
| A1 | `register_идэвхтэйОюутныгБүртгэхэд_OKБуцаажИмэйлИлгэнэ` | `when` + `verify` |
| A2 | `register_байхгүйОюутанд_ERROR_NO_STUDENTБуцаажХадгалахгүй` | `verify(..., never())` |
| A3 | `register_идэвхгүйОюутанд_ERROR_INACTIVE_STUDENTБуцаажИмэйлИлгээхгүй` | `verifyNoInteractions` |
| A4 | `register_өмнөхХолбооДутууБол_ERROR_PREREQUISITESБуцаажХадгалахгүй` | `when` + `never` |
| A5 | `register_имэйлИлгээгчАлдааШидэхэд_Бүртгэл_OKХэвээрХадгалагдана` | `doThrow(...).when(...)` |
| A6 | `register_nullОролтод_IllegalArgumentExceptionШидэжХамаарлаасХарьцахгүй` | `assertThrows` + `verifyNoInteractions` |
| A7 | `register_илгээгдсэнИмэйлийнБиед_ХичээлийнКодАгуулагдана` | `ArgumentCaptor` |
| A8 | `register_алдааныГурванТохиолдолд_ХаргалзахАлдааныКодБуцаана` | `@ParameterizedTest` + `@CsvSource` |

Тестийн нэр бүр `юуг_ямарНөхцөлд_юуХүлээгдэнэ` хэв маягтай.
Matcher-ийн дүрэм: нэг аргументад matcher хэрэглэсэн бол тухайн `verify` доторх бүх
аргументад matcher хэрэглэсэн (жишээ нь A7:
`verify(emailSender).send(eq(EMAIL), eq(SUBJECT), bodyCaptor.capture())`).

A6 нь хоёр аргументын null-ийг тусад нь шалгана:
`service.register(null, COURSE_ID)` ба `service.register(STUDENT_ID, null)` — хоёулаа
`IllegalArgumentException` шидэж, сан болон и-мэйл илгээгчтэй огт харьцахгүй байх ёстой.

### A8 дэх `lenient()` — яагаад хэрэгтэй болсон бэ

A8-ийн гурван CSV мөр нэг метод дотор нэг бүлэг stub хуваалцдаг ч мөр бүр зөвхөн нэг
хэсгийг нь ашигладаг (жишээ нь `ERROR_NO_STUDENT` мөр `courseExists`/`prerequisites` руу
хүрэхгүй), тиймээс MockitoExtension-ийн strict stubs горим ашиглагдаагүй stub-ыг
`UnnecessaryStubbingException` гэж үзэж тестийг унагахаас сэргийлж `lenient()` ашигласан.
`lenient()` нь зөвхөн «ашиглагдаагүй байж болно» гэсэн зөвшөөрөл; үр дүнгийн `verify`
шалгалтууд хэвээр хатуу хэвээр байна.

## 5. Хэсэг B — тестээ тестлэх (мини мутаци)

Мутант бүрийн дараа `src/main`-ыг эх хувилбар руу нь сэргээж, дараагийн мутацийг хийсэн.
Эцэст нь `results/mvn-test.txt`-ийг дахин ногооноор гаргасан.

| Мутант | `CourseRegistrationService.java`-д хийсэн эвдрэл | Унасан тестүүд | Баримт файл |
|---|---|---|---|
| 1 | `if (!student.active()) return "ERROR_INACTIVE_STUDENT";` мөрийг комментлож идэвхгүй оюутны шалгалтыг устгасан | A3 `..._ERROR_INACTIVE_STUDENT...` (`but was: <ERROR_NO_COURSE>`), A8-ийн 2-р мөр (`but was: <OK>`) — нийт 2 failure | `results/mvn-test-mutant-1.txt` |
| 2 | `repository.saveRegistration(studentId, courseId);` дуудалтыг комментлосон | A1 `..._OKБуцаажИмэйлИлгэнэ`, A5 `..._Бүртгэл_OKХэвээр...` — `verify(saveRegistration)` хангагдаагүй, нийт 2 failure | `results/mvn-test-mutant-2.txt` |
| 3 | `register()`-ийн эхний `if`-ээс `\|\| courseId == null` хэсгийг устгасан | A6 `register_nullОролтод_...` — `Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown` — 1 failure | `results/mvn-test-mutant-3.txt` |

Мутант 3-ыг A6 анхнаасаа хоёр аргументын null-ийг шалгадаг байсан тул шууд барьсан —
нэмж засах шаардлага гараагүй. Гурван мутантын гурвуулаа тестээр баригдсан нь
A1–A8-ийн assertion, `verify`-үүд үнэхээр ажиллаж байгааг харуулна. Мутацийн үед зарим
тест `Failures` биш `Errors` (жишээ нь `UnnecessaryStubbingException`) өгч унах нь ч
«тест унасан»-д тооцогдоно — энэ удаад гурван мутант бүгд цэвэр `Failures`-ээр унасан.

## 6. Дүгнэлт

Mock объект нь хамаарлын **гэрээг** (интерфейсийн зан төлөвийг) дуурайдаг тул тест нь
сан эсвэл SMTP серверийн бодит төлөв, сүлжээний саатал, өгөгдлийн буруу хэлбэр зэргийг
шалгахгүй — харин гараар бичсэн fake нь бодит логик (жишээ нь санах ойн map, эсвэл
бодит файлд бичих) агуулдаг учраас илүү олон алдаа барих боломжтой ч засвар үйлчилгээ
нь илүү үнэтэй. Энэ лабораторид repository, emailSender нь гадаад хилийн хамаарал тул
mock хийх нь зөв сонголт байсан. Харин `Student` record, `List.removeAll` зэрэг
өөрийн төслийн энгийн утгын объект, тооцооллыг mock хийх ёсгүй — тэдгээрийг бодит
объектоор ашиглах нь логикийг бүрэн шалгана. `verify`-ийг хэтрүүлбэл тест нь бизнес
үр дүнг биш, дотоод дуудалтын дарааллыг чанга тогтоож, кодын хэвийн рефакторинг
(жишээ нь кэш нэмэх, дуудалтыг зэрэгцүүлэх) бүрд унадаг хэврэг тест болно. Тиймээс
`verify`-ийг зөвхөн гэрээний чухал нөлөө (хадгалсан эсэх, и-мэйл явсан эсэх) дээр
ашиглаж, дотоод дарааллыг шаардлагагүй бол шалгахгүй байх нь дээр. Мутаци туршилт нь
«ногоон тест» гэдэг нь «сайн тест» гэсэн үг биш гэдгийг харуулсан: зөвхөн үр дүнг
шалгадаг сул тест нь кодын эвдрэлийг анзаарахгүй өнгөрдөг.

## 7. Сонголт 2 — AI өнцөг

`CourseRegistrationService.java`-г AI туслахад өгч «Mockito-гоор бүрэн тестийн класс бич»
гэж хүссэн. AI-ийн гаргасан класс `ai-review/AiCourseRegistrationServiceTest.java`-д
байгаа бөгөөд Хэсэг A-гийн suite-д ороогүй; дараах байдлаар тусад нь ажиллуулж шалгасан:

```bash
cp ai-review/AiCourseRegistrationServiceTest.java src/test/java/mn/edu/must/sqat/
mvn -Dtest=AiCourseRegistrationServiceTest test
rm src/test/java/mn/edu/must/sqat/AiCourseRegistrationServiceTest.java
```

AI-ийн тестүүд анхны код дээр 8/8 ногоон ажилласан (`results/ai-review/ai-tests-run.txt`).
Тохиолдлын хувьд AI нь **байхгүй оюутан, идэвхгүй оюутан, байхгүй хичээл, өмнөх холбоо
дутуу, өмнөх холбоо бүрэн хангагдсан, и-мэйл алдаа, null `studentId`** гэсэн замуудыг
бичсэн боловч `courseId == null` тохиолдлыг, бүртгэл үнэхээр хадгалагдсан
эсэхийг шалгах `verify(repository).saveRegistration(...)`-ийг, мөн и-мэйлийн агуулгыг
`ArgumentCaptor`-оор шалгахыг мартаж, харин бидний тестэд байгаагүй «бүх өмнөх холбоо
хангагдсан амжилттай бүртгэл» болон «exception-ийн мессеж» гэсэн санааг нэмж санасан.
Assertion-уудын хувьд буцах код (`OK`, `ERROR_*`) болон `verify(..., never())` нь
жинхэнэ зүйл шалгаж байгаа ч `verify(emailSender).send(anyString(), anyString(), anyString())`
мэт matcher бүх аргументыг үл харгалздаг тул бие хоосон, хаяг буруу байсан ч ногоон
өнгөрөх **сул тест** бий. Хэсэг B-гийн мутацуудыг AI-ийн тестүүд бүрэн барьж чадаагүй:
мутант 1-ийг `inactiveStudentShouldReturnErrorInactiveStudent` барьсан ч
(`results/ai-review/ai-tests-mutant-1.txt`, 1 failure), мутант 2 болон мутант 3-ыг
барьж чадалгүй BUILD SUCCESS-ээр өнгөрсөн (`ai-tests-mutant-2.txt`, `ai-tests-mutant-3.txt`).
Өөрөөр хэлбэл AI 8 тест бичсэн ч манай 10 тест 3/3 мутантыг барьсан бол AI 1/3-ыг л
барьсан — шалгах шалгуурыг зөв сонгох нь тестийн тооноос илүү чухал гэдгийг энэ
харьцуулалт харуулж байна.

## 8. GitHub ба .gitignore

`target/`, `.idea/`, `*.iml`, `.vscode/`, `.DS_Store` зэрэг нь `.gitignore`-д орсон.
Лабораторийн зааврын `.docx` файл репод ороогүй. `results/` хавтас (ногоон гаралт ба
гурван мутантын лог, AI-ийн харьцуулалтын лог) репод бүрэн орсон.
