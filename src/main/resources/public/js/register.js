// File: src/main/resources/public/js/register.js
document.addEventListener("DOMContentLoaded", () => {
    const registerForm = document.getElementById("registerForm");
    const registerSpinner = document.getElementById("registerSpinner");

    registerForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const nombre = document.getElementById("nombre").value;
        const apellido = document.getElementById("apellido").value;
        const email = document.getElementById("email").value;
        const password = document.getElementById("password").value;
        const confirmPassword = document.getElementById("confirmPassword").value;

        if (password !== confirmPassword) {
            showAlert("danger", "Las contraseñas no coinciden");
            return;
        }

        registerSpinner.classList.remove("d-none");

        try {
            const response = await fetch("/api/auth/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ nombre, apellido, email, password })
            });

            if (response.ok) {
                showAlert("success", "¡Registro exitoso! Redirigiendo...");
                setTimeout(() => window.location.href = "login.html", 2000);
            } else {
                const error = await response.json();
                showAlert("danger", error.message || "Error en el registro");
            }
        } catch (error) {
            showAlert("danger", "Error de conexión");
        } finally {
            registerSpinner.classList.add("d-none");
        }
    });

    function showAlert(type, message) {
        const existingAlert = document.querySelector(".alert");
        if (existingAlert) existingAlert.remove();

        const alertDiv = document.createElement("div");
        alertDiv.className = `alert alert-${type} mt-3`;
        alertDiv.textContent = message;
        registerForm.appendChild(alertDiv);
    }
});