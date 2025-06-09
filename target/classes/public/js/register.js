// File: src/main/resources/public/js/register.js
document.getElementById("registerForm").addEventListener("submit", async function (e) {
  e.preventDefault();
  const username = document.getElementById("username").value;
  const password = document.getElementById("password").value;

  const res = await fetch("/api/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, passwordHash: password })
  });

  const data = await res.json();
  const msg = document.getElementById("message");
  if (data.success) {
    msg.innerHTML = `<div class="alert alert-success">Registration successful. You can now <a href="/login">login</a>.</div>`;
  } else {
    msg.innerHTML = `<div class="alert alert-danger">Registration failed.</div>`;
  }
});