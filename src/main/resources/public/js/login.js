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
                body: JSON.stringify({ email : email, password : password })
            });

            // Always attempt to parse JSON.
            const data = await response.json();

            // Check response.ok for success (2xx status codes)
            if (response.ok) { // This handles status 200 explicitly
                // Your Java backend for successful login sets `success: true`
                // and includes the user object.
                alert('Login exitoso!.');
                window.location.href = '/';
            } else {
                // For non-2xx status codes (400, 401, 500),
                // your Java backend returns `success: false` and `message`
                alert(data.message || 'Error en el login. Por favor intenta nuevamente.');
            }
        } catch (error) {
            // This catch block is for network errors or if response.json() fails
            console.error('Error:', error);
            alert('Error de conexión. Por favor intenta nuevamente.');
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