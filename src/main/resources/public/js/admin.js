// File: src/main/resources/js/admin.js
document.addEventListener('DOMContentLoaded', () => {
    // Elementos del DOM
    const usersTableBody = document.getElementById('usersTableBody');
    const braceletsTableBody = document.getElementById('braceletsTableBody');
    const usersSpinner = document.getElementById('usersSpinner');
    const braceletsSpinner = document.getElementById('braceletsSpinner');

    // Modales
    const userModal = new bootstrap.Modal(document.getElementById('userModal'));
    const braceletModal = new bootstrap.Modal(document.getElementById('braceletModal'));
    const confirmModal = new bootstrap.Modal(document.getElementById('confirmModal'));

    // Variables de estado
    let currentAction = '';
    let currentItemId = '';

    // Inicialización
    checkAdminStatus();
    loadUsers();
    loadBracelets();

    // Event Listeners
    document.getElementById('addUserBtn').addEventListener('click', () => showUserModal('add'));
    document.getElementById('addBraceletBtn').addEventListener('click', () => showBraceletModal('add'));
    document.getElementById('saveUserBtn').addEventListener('click', saveUser);
    document.getElementById('saveBraceletBtn').addEventListener('click', saveBracelet);
    document.getElementById('confirmActionBtn').addEventListener('click', confirmAction);

    // Verificar estado de administrador
    function checkAdminStatus() {
        fetch('/api/auth/status')
            .then(response => response.json())
            .then(data => {
                if (!data.authenticated || data.role !== 'admin') {
                    window.location.href = '/';
                }
            })
            .catch(error => {
                console.error('Error checking admin status:', error);
                window.location.href = '/';
            });
    }

    // Cargar usuarios
    function loadUsers() {
        usersSpinner.style.display = 'block';
        usersTableBody.innerHTML = '';

        fetch('/api/admin/usuarios')
            .then(response => {
                if (!response.ok) throw new Error('Error al cargar usuarios');
                return response.json();
            })
            .then(users => {
                users.forEach(user => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${user._id.substring(18)}</td>
                        <td>${user.nombreUsuario || 'N/A'}</td>
                        <td>${user.correo || 'N/A'}</td>
                        <td>${user.rol || 'user'}</td>
                        <td class="action-buttons">
                            <button class="btn btn-sm btn-warning edit-user" data-id="${user._id}">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button class="btn btn-sm btn-danger delete-user" data-id="${user._id}">
                                <i class="bi bi-trash"></i>
                            </button>
                        </td>
                    `;
                    usersTableBody.appendChild(row);
                });

                // Agregar event listeners a los botones
                document.querySelectorAll('.edit-user').forEach(btn => {
                    btn.addEventListener('click', () => showUserModal('edit', btn.dataset.id));
                });

                document.querySelectorAll('.delete-user').forEach(btn => {
                    btn.addEventListener('click', () => showConfirmModal('deleteUser', btn.dataset.id, '¿Estás seguro de que deseas eliminar este usuario?'));
                });
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error al cargar usuarios');
            })
            .finally(() => {
                usersSpinner.style.display = 'none';
            });
    }

    // Cargar pulseras
    function loadBracelets() {
        braceletsSpinner.style.display = 'block';
        braceletsTableBody.innerHTML = '';

        fetch('/api/pulseras')
            .then(response => {
                if (!response.ok) throw new Error('Error al cargar pulseras');
                return response.json();
            })
            .then(bracelets => {
                bracelets.forEach(bracelet => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${bracelet._id.substring(18)}</td>
                        <td>${bracelet.nombre || 'Sin nombre'}</td>
                        <td>$${bracelet.precio?.toFixed(2) || '0.00'}</td>
                        <td>${bracelet.userBuilt ? 'Personalizada' : 'Predefinida'}</td>
                        <td>${bracelet.delisted ? 'Retirada' : 'Disponible'}</td>
                        <td class="action-buttons">
                            <button class="btn btn-sm btn-warning edit-bracelet" data-id="${bracelet._id}">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button class="btn btn-sm btn-danger delete-bracelet" data-id="${bracelet._id}">
                                <i class="bi bi-trash"></i>
                            </button>
                        </td>
                    `;
                    braceletsTableBody.appendChild(row);
                });

                // Agregar event listeners a los botones
                document.querySelectorAll('.edit-bracelet').forEach(btn => {
                    btn.addEventListener('click', () => showBraceletModal('edit', btn.dataset.id));
                });

                document.querySelectorAll('.delete-bracelet').forEach(btn => {
                    btn.addEventListener('click', () => showConfirmModal(
                        bracelet.delisted ? 'activateBracelet' : 'deleteBracelet',
                        btn.dataset.id,
                        bracelet.delisted ?
                            '¿Estás seguro de que deseas reactivar esta pulsera?' :
                            '¿Estás seguro de que deseas retirar esta pulsera?'
                    ));
                });
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error al cargar pulseras');
            })
            .finally(() => {
                braceletsSpinner.style.display = 'none';
            });
    }

    // Mostrar modal de usuario
    function showUserModal(action, userId = null) {
        const modalTitle = document.getElementById('userModalTitle');
        const passwordFields = document.getElementById('passwordFields');

        if (action === 'add') {
            modalTitle.textContent = 'Agregar Nuevo Usuario';
            document.getElementById('userForm').reset();
            passwordFields.style.display = 'block';
            document.getElementById('userPassword').required = true;
            currentAction = 'addUser';
        } else {
            modalTitle.textContent = 'Editar Usuario';
            passwordFields.style.display = 'block';
            document.getElementById('userPassword').required = false;
            currentAction = 'editUser';
            currentItemId = userId;

            // Cargar datos del usuario
            fetch(`/api/admin/usuario/${userId}`)
                .then(response => response.json())
                .then(user => {
                    document.getElementById('userId').value = user._id;
                    document.getElementById('userName').value = user.nombreUsuario;
                    document.getElementById('userEmail').value = user.correo;
                    document.getElementById('userRole').value = user.rol;
                })
                .catch(error => {
                    console.error('Error al cargar usuario:', error);
                    alert('Error al cargar datos del usuario');
                });
        }

        userModal.show();
    }

    // Mostrar modal de pulsera
    function showBraceletModal(action, braceletId = null) {
        const modalTitle = document.getElementById('braceletModalTitle');

        if (action === 'add') {
            modalTitle.textContent = 'Agregar Nueva Pulsera';
            document.getElementById('braceletForm').reset();
            currentAction = 'addBracelet';
        } else {
            modalTitle.textContent = 'Editar Pulsera';
            currentAction = 'editBracelet';
            currentItemId = braceletId;

            // Cargar datos de la pulsera
            fetch(`/api/pulseras/${braceletId}`)
                .then(response => response.json())
                .then(bracelet => {
                    document.getElementById('braceletId').value = bracelet._id;
                    document.getElementById('braceletName').value = bracelet.nombre;
                    document.getElementById('braceletDescription').value = bracelet.descripcion;
                    document.getElementById('braceletPrice').value = bracelet.precio;
                    document.getElementById('braceletStatus').value = bracelet.delisted;
                })
                .catch(error => {
                    console.error('Error al cargar pulsera:', error);
                    alert('Error al cargar datos de la pulsera');
                });
        }

        braceletModal.show();
    }

    // Mostrar modal de confirmación
    function showConfirmModal(action, itemId, message) {
        currentAction = action;
        currentItemId = itemId;
        document.getElementById('confirmModalBody').textContent = message;
        confirmModal.show();
    }

    // Guardar usuario
    function saveUser() {
        const userId = document.getElementById('userId').value;
        const userData = {
            nombreUsuario: document.getElementById('userName').value,
            correo: document.getElementById('userEmail').value,
            rol: document.getElementById('userRole').value
        };

        const password = document.getElementById('userPassword').value;
        if (password) {
            userData.password = password;
        }

        const url = currentAction === 'addUser' ? '/api/admin/usuario' : `/api/admin/usuario/${userId}`;
        const method = currentAction === 'addUser' ? 'POST' : 'PUT';

        fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(userData)
        })
        .then(response => {
            if (!response.ok) throw new Error('Error al guardar usuario');
            return response.json();
        })
        .then(() => {
            alert('Usuario guardado correctamente');
            userModal.hide();
            loadUsers();
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error al guardar usuario');
        });
    }

    // Guardar pulsera
    function saveBracelet() {
        const braceletId = document.getElementById('braceletId').value;
        const braceletData = {
            nombre: document.getElementById('braceletName').value,
            descripcion: document.getElementById('braceletDescription').value,
            precio: parseFloat(document.getElementById('braceletPrice').value),
            delisted: document.getElementById('braceletStatus').value === 'true'
        };

        const url = currentAction === 'addBracelet' ? '/api/pulseras' : `/api/pulseras/${braceletId}`;
        const method = currentAction === 'addBracelet' ? 'POST' : 'PUT';

        fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(braceletData)
        })
        .then(response => {
            if (!response.ok) throw new Error('Error al guardar pulsera');
            return response.json();
        })
        .then(() => {
            alert('Pulsera guardada correctamente');
            braceletModal.hide();
            loadBracelets();
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error al guardar pulsera');
        });
    }

    // Confirmar acción (eliminar/activar)
    function confirmAction() {
        confirmModal.hide();

        let url, method;

        switch(currentAction) {
            case 'deleteUser':
                url = `/api/admin/usuario/${currentItemId}`;
                method = 'DELETE';
                break;

            case 'deleteBracelet':
                url = `/api/pulseras/${currentItemId}`;
                method = 'DELETE';
                break;

            case 'activateBracelet':
                url = `/api/pulseras/${currentItemId}`;
                method = 'PUT';
                break;

            default:
                return;
        }

        fetch(url, { method })
            .then(response => {
                if (!response.ok) throw new Error('Error al realizar la acción');
                return response.json();
            })
            .then(() => {
                alert('Acción realizada correctamente');
                if (currentAction.includes('User')) {
                    loadUsers();
                } else {
                    loadBracelets();
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error al realizar la acción');
            });
    }
});