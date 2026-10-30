package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AI туслахын бүрэн гэж үзсэн Mockito тестийн класс — Хэсэг A-гийн 10 тестийн
 * бүрэлдэхүүнд ОРООГҮЙ. Харьцуулалтын баримт болгон ai-review/ хавтаст хадгалсан;
 * ажиллуулах зааврыг README-ийн «Сонголт 2» хэсгээс үз.
 */
@ExtendWith(MockitoExtension.class)
class AiCourseRegistrationServiceTest {

    @Mock
    private StudentRepository repository;

    @Mock
    private EmailSender emailSender;

    private CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    private Student student(boolean active, String... coursesTaken) {
        return new Student("S1", "s1@must.edu.mn", active, List.of(coursesTaken));
    }

    @Test
    void activeStudentWithNoPrerequisitesShouldRegisterSuccessfully() {
        when(repository.findStudent("S1")).thenReturn(student(true));
        when(repository.courseExists("CS101")).thenReturn(true);
        when(repository.prerequisites("CS101")).thenReturn(List.of());

        String result = service.register("S1", "CS101");

        assertEquals("OK", result);
        verify(repository).courseExists("CS101");
        verify(emailSender).send(anyString(), anyString(), anyString());
    }

    @Test
    void unknownStudentShouldReturnErrorNoStudent() {
        when(repository.findStudent("S1")).thenReturn(null);

        assertEquals("ERROR_NO_STUDENT", service.register("S1", "CS101"));
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void inactiveStudentShouldReturnErrorInactiveStudent() {
        when(repository.findStudent("S1")).thenReturn(student(false));

        assertEquals("ERROR_INACTIVE_STUDENT", service.register("S1", "CS101"));
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void nonExistingCourseShouldReturnErrorNoCourse() {
        when(repository.findStudent("S1")).thenReturn(student(true));
        when(repository.courseExists("CS999")).thenReturn(false);

        assertEquals("ERROR_NO_COURSE", service.register("S1", "CS999"));
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    @Test
    void missingPrerequisiteShouldReturnErrorPrerequisites() {
        when(repository.findStudent("S1")).thenReturn(student(true, "CS100"));
        when(repository.courseExists("CS101")).thenReturn(true);
        when(repository.prerequisites("CS101")).thenReturn(List.of("CS100", "CS102"));

        assertEquals("ERROR_PREREQUISITES", service.register("S1", "CS101"));
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void studentWithAllPrerequisitesTakenShouldRegisterSuccessfully() {
        when(repository.findStudent("S1")).thenReturn(student(true, "CS100", "CS102"));
        when(repository.courseExists("CS101")).thenReturn(true);
        when(repository.prerequisites("CS101")).thenReturn(List.of("CS100", "CS102"));

        assertEquals("OK", service.register("S1", "CS101"));
        verify(emailSender).send(anyString(), anyString(), anyString());
    }

    @Test
    void emailSenderFailureShouldNotAffectRegistration() {
        when(repository.findStudent("S1")).thenReturn(student(true));
        when(repository.courseExists("CS101")).thenReturn(true);
        when(repository.prerequisites("CS101")).thenReturn(List.of());
        doThrow(new RuntimeException("SMTP down"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        assertEquals("OK", service.register("S1", "CS101"));
    }

    @Test
    void nullStudentIdShouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.register(null, "CS101"));

        assertTrue(ex.getMessage().contains("шаардлагатай"));
    }
}
