package com.smartcampus.examgrading.view;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.Exam;
import com.smartcampus.examgrading.model.Grade;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.service.ExamService;
import com.smartcampus.examgrading.service.GradeService;
import com.smartcampus.examgrading.service.SessionService;
import com.smartcampus.examgrading.service.StudentService;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Route("grade-management")
@PageTitle("Grade Management | Smart Campus System")
public class FacultyGradeView extends VerticalLayout implements BeforeEnterObserver {

    private final GradeService gradeService;
    private final ExamService examService;
    private final SessionService sessionService;
    private final StudentService studentService;

    // Add course selector
    private ComboBox<Course> courseComboBox = new ComboBox<>("Select Course");
    private ComboBox<Exam> examComboBox = new ComboBox<>("Select Exam");
    private Grid<User> studentGrid = new Grid<>(User.class, false);
    private Grid<Grade> gradeGrid = new Grid<>(Grade.class, false);

    private Course selectedCourse;
    private Exam selectedExam;
    private User currentUser;

    public FacultyGradeView(GradeService gradeService, ExamService examService,
            SessionService sessionService, StudentService studentService) {
        this.gradeService = gradeService;
        this.examService = examService;
        this.sessionService = sessionService;
        this.studentService = studentService;

        // Set layout properties
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Check if user is logged in and is faculty or admin
        if (!sessionService.isLoggedIn()) {
            return; // BeforeEnter will handle the redirect
        }

        this.currentUser = sessionService.getCurrentUser();

        if (!sessionService.isFaculty() && !sessionService.isAdmin()) {
            // Display access denied message
            add(new H2("Access Denied"),
                    new Paragraph("Only faculty members and administrators can manage grades."));
            return;
        }

        setupLayout();
    }

    private void setupLayout() {
        // Header
        add(new H2("Grade Management"));

        // Create a horizontal layout for selectors
        HorizontalLayout selectorLayout = new HorizontalLayout();
        selectorLayout.setWidthFull();
        selectorLayout.setSpacing(true);

        // Course selector
        courseComboBox.setWidthFull();
        courseComboBox.setItemLabelGenerator(course -> course.getCourseCode() + " - " + course.getCourseName());
        courseComboBox.addValueChangeListener(event -> {
            selectedCourse = event.getValue();
            if (selectedCourse != null) {
                updateExamComboBox(selectedCourse);
                // Clear exam selection when course changes
                examComboBox.clear();
                studentGrid.setItems(new ArrayList<>());
                gradeGrid.setItems(new ArrayList<>());
            }
        });

        // Exam selector
        examComboBox.setWidthFull();
        examComboBox.setItemLabelGenerator(exam -> exam.getExamName() + " (" + exam.getExamType() + ")");
        examComboBox.addValueChangeListener(event -> {
            selectedExam = event.getValue();
            if (selectedExam != null) {
                updateStudentGrid();
            }
        });

        selectorLayout.add(courseComboBox, examComboBox);
        selectorLayout.setFlexGrow(1, courseComboBox, examComboBox);

        add(selectorLayout);

        // Student grid for displaying students enrolled in the course
        configureStudentGrid();
        add(new H3("Students"), studentGrid);

        // Grades grid for displaying all grades for the selected exam
        configureGradeGrid();
        add(new H3("All Grades"), gradeGrid);

        // Load data
        loadCourses();
    }

    private void loadCourses() {
        // Get courses based on user role
        List<Course> courses;

        if (sessionService.isAdmin()) {
            // Admin can see all courses
            courses = examService.getAllCourses();
        } else {
            // Faculty can see only their courses
            courses = examService.getCoursesByFacultyId(currentUser.getUserId());
        }

        courseComboBox.setItems(courses);

        // Select first course by default if available
        if (!courses.isEmpty()) {
            courseComboBox.setValue(courses.get(0));
        }
    }

    private void updateExamComboBox(Course course) {
        if (course != null) {
            List<Exam> exams = examService.getExamsByCourseId(course.getCourseId());
            examComboBox.setItems(exams);

            // Select first exam by default if available
            if (!exams.isEmpty()) {
                examComboBox.setValue(exams.get(0));
            }
        } else {
            examComboBox.setItems(new ArrayList<>());
        }
    }

    private void configureStudentGrid() {
        studentGrid.addColumn(User::getUsername).setHeader("Username").setAutoWidth(true);
        studentGrid.addColumn(User::getFirstName).setHeader("First Name").setAutoWidth(true);
        studentGrid.addColumn(User::getLastName).setHeader("Last Name").setAutoWidth(true);
        studentGrid.addColumn(User::getEmail).setHeader("Email").setAutoWidth(true);

        // Add column for grade status
        studentGrid.addColumn(student -> {
            if (selectedExam == null)
                return "No exam selected";

            Optional<Grade> grade = gradeService.getGradeByStudentAndExam(student.getUserId(), selectedExam.getId());
            return grade.isPresent() ? "Graded" : "Not Graded";
        }).setHeader("Grade Status").setAutoWidth(true);

        // Add column for actions
        studentGrid.addColumn(new ComponentRenderer<>(student -> {
            Button gradeBtn = new Button("Enter Grade", new Icon(VaadinIcon.PENCIL));
            gradeBtn.addClickListener(e -> openGradeDialog(student));
            return gradeBtn;
        })).setHeader("Actions").setAutoWidth(true);

        studentGrid.setHeight("300px");
    }

    private void configureGradeGrid() {
        gradeGrid.addColumn(grade -> {
            User student = grade.getStudent();
            return student != null ? student.getFirstName() + " " + student.getLastName() : "Unknown";
        }).setHeader("Student").setAutoWidth(true);

        gradeGrid.addColumn(grade -> grade.getMarksObtained().toString()).setHeader("Marks").setAutoWidth(true);
        gradeGrid.addColumn(grade -> String.format("%.2f%%", grade.getPercentage())).setHeader("Percentage")
                .setAutoWidth(true);

        gradeGrid.addColumn(Grade::getGradeLetter).setHeader("Grade").setAutoWidth(true);
        gradeGrid.addColumn(Grade::getFeedback).setHeader("Feedback").setAutoWidth(true);

        gradeGrid.addColumn(grade -> {
            User grader = grade.getGrader();
            return grader != null ? grader.getUsername() : "Unknown";
        }).setHeader("Graded By").setAutoWidth(true);

        gradeGrid.addColumn(
                grade -> grade.getGradedAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .setHeader("Graded At").setAutoWidth(true);

        // Add edit button
        gradeGrid.addColumn(new ComponentRenderer<>(grade -> {
            Button editBtn = new Button("Edit", new Icon(VaadinIcon.EDIT));
            editBtn.addClickListener(e -> {
                User student = grade.getStudent();
                if (student != null) {
                    openGradeDialog(student);
                }
            });
            return editBtn;
        })).setHeader("Actions").setAutoWidth(true);

        gradeGrid.setHeight("300px");
    }

    private void updateStudentGrid() {
        if (selectedExam != null && selectedCourse != null) {
            // Get all students enrolled in this course
            List<User> enrolledStudents = studentService.getStudentsEnrolledInCourse(selectedCourse.getCourseId());
            studentGrid.setItems(enrolledStudents);

            // Update grades grid
            updateGradeGrid();
        }
    }

    private void updateGradeGrid() {
        if (selectedExam != null) {
            List<Grade> grades = gradeService.getGradesByExamId(selectedExam.getId());
            gradeGrid.setItems(grades);
        } else {
            gradeGrid.setItems();
        }
    }

    private void openGradeDialog(User student) {
        if (selectedExam == null) {
            Notification.show("Please select an exam first", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Enter Grade for " + student.getFirstName() + " " + student.getLastName());

        // Form layout
        FormLayout formLayout = new FormLayout();

        // Get existing grade, if any
        Optional<Grade> existingGradeOpt = gradeService.getGradeByStudentAndExam(
                student.getUserId(), selectedExam.getId());

        Grade grade = existingGradeOpt.orElseGet(() -> {
            Grade newGrade = new Grade();
            newGrade.setStudentId(student.getUserId());
            newGrade.setExamId(selectedExam.getId());
            newGrade.setMarksObtained(BigDecimal.ZERO);
            newGrade.setGradedBy(currentUser.getUserId()); // Set current faculty as grader
            return newGrade;
        });

        // Form fields
        NumberField marksField = new NumberField("Marks Obtained");
        marksField.setValue(grade.getMarksObtained().doubleValue());
        marksField.setMin(0);
        marksField.setMax(selectedExam.getTotalMarks().doubleValue());
        marksField.setStep(0.5);
        marksField.setWidthFull();

        TextArea feedbackField = new TextArea("Feedback");
        feedbackField.setValue(grade.getFeedback() != null ? grade.getFeedback() : "");
        feedbackField.setWidthFull();
        feedbackField.setHeight("150px");

        formLayout.add(marksField, feedbackField);
        dialog.add(formLayout);

        // Buttons
        Button saveButton = new Button("Save", e -> {
            if (marksField.isEmpty()) {
                Notification.show("Please enter marks", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                grade.setMarksObtained(BigDecimal.valueOf(marksField.getValue()));
                grade.setFeedback(feedbackField.getValue());

                gradeService.saveGrade(grade);

                Notification.show("Grade saved successfully", 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                dialog.close();
                updateStudentGrid();
                updateGradeGrid();
            } catch (Exception ex) {
                Notification.show("Error saving grade: " + ex.getMessage(),
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Redirect to login if not logged in
        if (!sessionService.isLoggedIn()) {
            event.forwardTo(LoginView.class);
            return;
        }

        // Redirect students to the appropriate view
        if (sessionService.isStudent()) {
            event.forwardTo(StudentGradeView.class);
        }
    }
}