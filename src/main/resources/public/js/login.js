// File: src/main/resources/public/js/login.js
document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("loginForm");
    const loginSpinner = document.getElementById("loginSpinner");

    loginForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const email = document.getElementById("email").value;
        const password = document.getElementById("password").value;

        loginSpinner.classList.remove("d-none");

        try {
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password })
            });

            if (response.ok) {
                window.location.href = "/";
            } else {
                const error = await response.json();
                showAlert("danger", error.message || "Error al iniciar sesión");
            }
        } catch (error) {
            showAlert("danger", "Error de conexión");
        } finally {
            loginSpinner.classList.add("d-none");
        }
    });

    function showAlert(type, message) {
        const alertDiv = document.createElement("div");
        alertDiv.className = `alert alert-${type} mt-3`;
        alertDiv.textContent = message;
        loginForm.appendChild(alertDiv);

        setTimeout(() => alertDiv.remove(), 5000);
    }
});