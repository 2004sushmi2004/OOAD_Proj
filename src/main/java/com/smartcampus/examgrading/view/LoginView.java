package com.smartcampus.examgrading.view;

import com.smartcampus.examgrading.service.AuthService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.component.html.Image;

@Route("login")
@RouteAlias("")
@PageTitle("Login | Smart Campus System")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthService authService;

    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Button loginButton;

    public LoginView(AuthService authService) {
        this.authService = authService;

        // Set layout properties
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Create a container for the login form
        Div loginContainer = new Div();
        loginContainer.getStyle()
                .set("background-color", "white")
                .set("border-radius", "8px")
                .set("box-shadow", "0 4px 8px rgba(0, 0, 0, 0.1)")
                .set("padding", "32px")
                .set("max-width", "400px")
                .set("width", "100%");

        // Add logo
        Image logo = new Image("images/logo.png", "Smart Campus Logo");
        logo.setHeight("64px");
        logo.getStyle().set("margin-bottom", "24px");

        // Add header
        H2 header = new H2("Smart Campus System");
        header.getStyle().set("margin-top", "0");

        // Create form components
        usernameField = new TextField("Username");
        usernameField.setWidthFull();
        usernameField.setRequired(true);
        usernameField.setAutofocus(true);

        passwordField = new PasswordField("Password");
        passwordField.setWidthFull();
        passwordField.setRequired(true);

        loginButton = new Button("Login");
        loginButton.addClickListener(e -> login());
        loginButton.getStyle()
                .set("margin-top", "16px")
                .set("background-color", "#1565C0")
                .set("color", "white");
        loginButton.setWidthFull();

        // Create layout for form components
        VerticalLayout formLayout = new VerticalLayout(
                usernameField,
                passwordField,
                loginButton);
        formLayout.setPadding(false);
        formLayout.setSpacing(true);
        formLayout.setWidthFull();

        // Add everything to the container
        loginContainer.add(logo, header, formLayout);

        // Add the container to the main layout
        add(loginContainer);

        // Set background color for the main layout
        getStyle()
                .set("background-color", "#f5f5f5");
    }

    private void login() {
        String username = usernameField.getValue();
        String password = passwordField.getValue();

        if (username.isEmpty() || password.isEmpty()) {
            showNotification("Please enter both username and password", NotificationVariant.LUMO_ERROR);
            return;
        }

        boolean success = authService.login(username, password);

        if (success) {
            // Redirect based on user role - will be handled in BeforeEnterObserver
            getUI().ifPresent(ui -> ui.navigate("dashboard"));
        } else {
            showNotification("Invalid username or password", NotificationVariant.LUMO_ERROR);
            passwordField.setValue("");
            usernameField.focus();
        }
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = new Notification(message, 3000);
        notification.addThemeVariants(variant);
        notification.open();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Check if user is already logged in, redirect if needed
        // This would typically check with SessionService to see if there's an active
        // session
        // For this example, we're just showing the login page
    }
}