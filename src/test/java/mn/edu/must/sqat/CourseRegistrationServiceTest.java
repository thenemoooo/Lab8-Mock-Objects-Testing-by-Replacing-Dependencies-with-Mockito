package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    private static final String STUDENT_ID = "B222270805";
    private static final String COURSE_ID = "CS313";
    private static final String EMAIL = "nyamsurennyamod8@gmail.com";
    private static final String SUBJECT = "Бүртгэл баталгаажлаа";

    @Mock
    StudentRepository repository;

    @Mock
    EmailSender emailSender;

    CourseRegistrationService service;

    private Student activeStudent;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
        activeStudent = new Student(STUDENT_ID, EMAIL, true, List.of("CS201"));
    }

    // A1
    @Test
    void register_идэвхтэйОюутныгБүртгэхэд_OKБуцаажИмэйлИлгэнэ() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent);
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS201"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(EMAIL, SUBJECT, COURSE_ID + " хичээлд амжилттай бүртгэгдлээ.");
    }

    // A2
    @Test
    void register_байхгүйОюутанд_ERROR_NO_STUDENTБуцаажХадгалахгүй() {
        when(repository.findStudent("UNKNOWN")).thenReturn(null);

        String result = service.register("UNKNOWN", COURSE_ID);

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // A3
    @Test
    void register_идэвхгүйОюутанд_ERROR_INACTIVE_STUDENTБуцаажИмэйлИлгээхгүй() {
        when(repository.findStudent(STUDENT_ID))
                .thenReturn(new Student(STUDENT_ID, EMAIL, false, List.of()));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verifyNoInteractions(emailSender);
    }

    // A4
    @Test
    void register_өмнөхХолбооДутууБол_ERROR_PREREQUISITESБуцаажХадгалахгүй() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent);
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS201", "CS202"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // A5
    @Test
    void register_имэйлИлгээгчАлдааШидэхэд_Бүртгэл_OKХэвээрХадгалагдана() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent);
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of());
        doThrow(new RuntimeException("SMTP сервер унав"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(EMAIL), anyString(), anyString());
    }

    // A6
    @Test
    void register_nullОролтод_IllegalArgumentExceptionШидэжХамаарлаасХарьцахгүй() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, COURSE_ID));
        assertThrows(IllegalArgumentException.class, () -> service.register(STUDENT_ID, null));

        verifyNoInteractions(repository, emailSender);
    }

    // A7
    @Test
    void register_илгээгдсэнИмэйлийнБиед_ХичээлийнКодАгуулагдана() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent);
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of());

        service.register(STUDENT_ID, COURSE_ID);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(EMAIL), eq(SUBJECT), bodyCaptor.capture());

        String body = bodyCaptor.getValue();
        assertTrue(body.contains(COURSE_ID), "Биед хичээлийн код байх ёстой: " + body);
        assertTrue(body.contains("амжилттай"), "Биед амжилтын мэдэгдэл байх ёстой: " + body);
    }

    // A8
    @ParameterizedTest(name = "[{index}] {0} / {1} → {2}")
    @CsvSource({
            "B222270805, CS313, ERROR_NO_STUDENT",
            "B222270806, CS313, ERROR_INACTIVE_STUDENT",
            "B222270807, CS999, ERROR_NO_COURSE"
    })
    void register_алдааныГурванТохиолдолд_ХаргалзахАлдааныКодБуцаана(
            String studentId, String courseId, String expected) {
        lenient().when(repository.findStudent(anyString()))
                .thenAnswer(invocation -> switch ((String) invocation.getArgument(0)) {
                    case "B222270805" -> null;
                    case "B222270806" -> new Student("B222270806", "inactive@must.edu.mn", false, List.of());
                    default -> new Student("B222270807", "active@must.edu.mn", true, List.of());
                });
        lenient().when(repository.courseExists(anyString()))
                .thenReturn(!courseId.equals("CS999"));
        lenient().when(repository.prerequisites(anyString())).thenReturn(List.of());

        String result = service.register(studentId, courseId);

        assertEquals(expected, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }
}
