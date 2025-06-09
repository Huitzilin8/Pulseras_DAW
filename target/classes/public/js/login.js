// File: src/main/resources/public/js/login.js
document.getElementById("loginForm").addEventListener("submit", async function (e) {
  e.preventDefault();
  const username = document.getElementById("username").value;
  const password = document.getElementById("password").value;

  const res = await fetch("/api/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, passwordHash: password })
  });

  const data = await res.json();
  const msg = document.getElementById("message");
  if (data.success) {
    msg.innerHTML = `<div class="alert alert-success">Login successful. Redirecting...</div>`;
    setTimeout(() => window.location.href = "/index.html", 1000);
  } else {
    msg.innerHTML = `<div class="alert alert-danger">Login failed.</div>`;
  }
});