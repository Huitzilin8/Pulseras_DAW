// File: src/main/resources/public/js/main.js

// Wait for the DOM to load
document.addEventListener("DOMContentLoaded", () => {
  const tableBody = document.querySelector("#usersTable tbody");
  const addBtn = document.getElementById("addBtn");
  const loginLink = document.getElementById("loginLink");
  const logoutLink = document.getElementById("logoutLink");

  // Load all users on page load
  fetchUsers();

  // Add user handler
  addBtn.addEventListener("click", () => {
    const username = prompt("Enter new username:");
    if (!username) return;
    const role = prompt("Enter role (admin/user):", "user");
    if (!role) return;
    const password = prompt("Enter initial password:");
    if (!password) return;

    // Send new user to backend
    fetch("/api/user", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username,
        role,
        passwordHash: password,
        registeredAt: new Date().toISOString(),
        lastLogin: new Date().toISOString()
      })
    }).then(res => {
      if (res.ok) {
        alert("User created");
        fetchUsers();
      } else {
        res.text().then(txt => alert("Failed to create user: " + txt));
      }
    });
  });

  // Fetch all users and render in table
  function fetchUsers() {
    fetch("/api/users")
      .then(res => res.json())
      .then(users => {
        tableBody.innerHTML = "";
        users.forEach(user => {
          const tr = document.createElement("tr");
          tr.innerHTML = `
            <td>${user.id}</td>
            <td><input type="text" class="form-control form-control-sm" value="${user.username}"></td>
            <td><input type="text" class="form-control form-control-sm" value="${user.role}"></td>
            <td>${new Date(user.registeredAt).toLocaleString()}</td>
            <td>${new Date(user.lastLogin).toLocaleString()}</td>
            <td>
              <button class="btn btn-sm btn-success">Save</button>
              <button class="btn btn-sm btn-danger">Delete</button>
            </td>`;
          const [usernameInput, roleInput] = tr.querySelectorAll("input");
          const [saveBtn, delBtn] = tr.querySelectorAll("button");

          // Save handler
          saveBtn.addEventListener("click", () => {
            fetch(`/api/user/${user.id}`, {
              method: "PUT",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({
                username: usernameInput.value,
                role: roleInput.value
              })
            }).then(res => {
              if (res.ok) {
                alert("User updated");
                fetchUsers();
              } else {
                res.text().then(txt => alert("Failed to update: " + txt));
              }
            });
          });

          // Delete handler
          delBtn.addEventListener("click", () => {
            if (!confirm("Are you sure?")) return;
            fetch(`/api/user/${user.id}`, { method: "DELETE" })
              .then(res => {
                if (res.ok) {
                  alert("User deleted");
                  fetchUsers();
                } else {
                  res.text().then(txt => alert("Failed to delete: " + txt));
                }
              });
          });

          tableBody.appendChild(tr);
        });
      });
  }
});
