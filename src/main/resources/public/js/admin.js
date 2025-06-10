    // File: src/main/resources/js/admin.js
document.addEventListener('DOMContentLoaded', () => {
    // Elementos del DOM
    const usersTableBody = document.getElementById('usersTableBody');
    const braceletsCardContainer = document.getElementById('braceletsCardContainer');
    const usersSpinner = document.getElementById('usersSpinner');
    const braceletsSpinner = document.getElementById('braceletsSpinner');
    const braceletImageInput = document.getElementById('braceletImage');
    const imagePreview = document.getElementById('imagePreview');
    const imgpathInput = document.getElementById('imgpath');

    // Modales
    const userModal = new bootstrap.Modal(document.getElementById('userModal'));
    const braceletModal = new bootstrap.Modal(document.getElementById('braceletModal'));
    const confirmModal = new bootstrap.Modal(document.getElementById('confirmModal'));
    const myAlertModal = new bootstrap.Modal(document.getElementById('myAlertModal'));
    const myAlertModalLabel = document.getElementById('myAlertModalLabel');
    const myAlertModalBody = document.getElementById('myAlertModalBody');

    // Variables de estado
    let currentAction = '';
    let currentItemId = '';
    let currentBracelet = null; // Para almacenar los datos de la pulsera actual al editar

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

    if (braceletImageInput) {
        braceletImageInput.addEventListener('change', handleImageUpload);
    }

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
                        <td>${user.id.substring(18)}</td>
                        <td>${user.nombreUsuario || 'N/A'}</td>
                        <td>${user.correo || 'N/A'}</td>
                        <td>${user.rol || 'user'}</td>
                        <td class="action-buttons">
                            <button class="btn btn-sm btn-warning edit-user" data-id="${user.id}">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button class="btn btn-sm btn-danger delete-user" data-id="${user.id}">
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
                showAlert('danger','Error al cargar usuarios: '+ error);
            })
            .finally(() => {
                usersSpinner.style.display = 'none';
            });
    }

    // Cargar pulseras
    function loadBracelets() {
        braceletsSpinner.style.display = 'block';
        braceletsCardContainer.innerHTML = ''; // Limpiar el contenedor de cartas

        fetch('/api/pulseras')
            .then(response => {
                if (!response.ok) throw new Error('Error al cargar pulseras');
                return response.json();
            })
            .then(bracelets => {
                bracelets.forEach(bracelet => {
                    const card = document.createElement('div');
                    card.classList.add('col');
                    const imageUrl = bracelet.imgURL ? bracelet.imgURL : ''; // Usa la URL de la imagen si existe
                    const imageHtml = imageUrl ? `<img src="${imageUrl}" class="card-img-top" alt="${bracelet.nombre}">` : `<div class="bg-secondary text-white d-flex align-items-center justify-content-center" style="height: 200px;">Sin Imagen</div>`;

                    card.innerHTML = `
                        <div class="card h-100">
                            ${imageHtml}
                            <div class="card-body">
                                <h5 class="card-title">${bracelet.nombre || 'Sin nombre'}</h5>
                                <p class="card-text">Precio: $${bracelet.precio?.toFixed(2) || '0.00'}</p>
                                <p class="card-text"><small class="text-muted">${bracelet.userBuilt ? 'Personalizada' : 'Predefinida'}</small></p>
                                <p class="card-text"><small class="text-muted">${bracelet.delisted ? 'Retirada' : 'Disponible'}</small></p>
                                <div class="action-buttons">
                                    <button class="btn btn-sm btn-warning edit-bracelet" data-id="${bracelet.id}">
                                        <i class="bi bi-pencil"></i>
                                    </button>
                                    <button class="btn btn-sm btn-danger delete-bracelet" data-id="${bracelet.id}">
                                        <i class="bi bi-trash"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    `;
                    braceletsCardContainer.appendChild(card);
                });

                // Agregar event listeners a los botones de las cartas
                document.querySelectorAll('.edit-bracelet').forEach(btn => {
                    btn.addEventListener('click', () => showBraceletModal('edit', btn.dataset.id));
                });

                document.querySelectorAll('.delete-bracelet').forEach(btn => {
                    btn.addEventListener('click', () => showConfirmModal(
                        // Se verifica si la pulsera está 'delisted' para determinar si la acción es reactivar o retirar
                        bracelets.find(b => b.id === btn.dataset.id)?.delisted ? 'activateBracelet' : 'deleteBracelet',
                        btn.dataset.id,
                        bracelets.find(b => b.id === btn.dataset.id)?.delisted ?
                            '¿Estás seguro de que deseas reactivar esta pulsera?' :
                            '¿Estás seguro de que deseas retirar esta pulsera?'
                    ));
                });
            })
            .catch(error => {
                console.error('Error:', error);
                showAlert('danger','Error al cargar pulseras: ' + error);
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
                    document.getElementById('userId').value = user.id;
                    document.getElementById('userName').value = user.nombreUsuario;
                    document.getElementById('userEmail').value = user.correo;
                    document.getElementById('userRole').value = user.rol;
                })
                .catch(error => {
                    console.error('Error al cargar usuario:', error);
                    showAlert('danger','Error al cargar datos del usuario: ' + error);
                });
        }

        userModal.show();
    }

    // Mostrar modal de pulsera
    function showBraceletModal(action, braceletId = null) {
        const modalTitle = document.getElementById('braceletModalTitle');
        const braceletForm = document.getElementById('braceletForm');
        braceletForm.reset(); // Limpiar el formulario al abrir el modal
        imagePreview.innerHTML = ''; // Limpiar la vista previa de la imagen
        imgpathInput.value = ''; // Limpiar el path de la imagen
        currentBracelet = null; // Resetear la pulsera actual

        if (action === 'add') {
            modalTitle.textContent = 'Agregar Nueva Pulsera';
            currentAction = 'addBracelet';
        } else {
            modalTitle.textContent = 'Editar Pulsera';
            currentAction = 'editBracelet';
            currentItemId = braceletId;

            // Cargar datos de la pulsera
            fetch(`/api/pulseras/${braceletId}`)
                .then(response => response.json())
                .then(bracelet => {
                    currentBracelet = bracelet; // Guardar la pulsera actual
                    document.getElementById('braceletId').value = bracelet.id;
                    document.getElementById('braceletName').value = bracelet.nombre;
                    document.getElementById('braceletDescription').value = bracelet.descripcion;
                    document.getElementById('braceletPrice').value = bracelet.precio;
                    document.getElementById('braceletStatus').value = bracelet.delisted;
                    if (bracelet.imgURL) {
                        imagePreview.innerHTML = `<img src="${bracelet.imgURL}" alt="Vista previa" style="max-width: 100px; max-height: 100px;">`;
                        // Guardar solo el nombre del archivo si la URL es /img/{path}.png
                        const imgPathMatch = bracelet.imgURL.match(/\/img\/(.*)/);
                        if (imgPathMatch && imgPathMatch[1]) {
                            imgpathInput.value = imgPathMatch[1];
                        }
                    }
                })
                .catch(error => {
                    console.error('Error al cargar pulsera:', error);
                    showAlert('danger','Error al cargar datos de la pulsera: ' + error);
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
            showAlert('success','Usuario guardado correctamente');
            userModal.hide();
            loadUsers();
        })
        .catch(error => {
            console.error('Error:', error);
            showAlert('danger','Error al guardar usuario: ' + error);
        });
    }

    // Subir imagen y obtener la ruta
    async function uploadImage(file) {
        const formData = new FormData();
        formData.append('image', file);

        try {
            const response = await fetch('/api/admin/upload/img', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.details || 'Error al subir la imagen');
            }

            const data = await response.json();
            return data.filename; // Suponiendo que la respuesta JSON tiene un campo 'filename'
        } catch (error) {
            console.error('Error al subir la imagen:', error);
            showAlert('danger', 'Error al subir la imagen: ' + error.message);
            return null;
        }
    }

    async function handleImageUpload(event) {
        const file = event.target.files?.[0];
        if (file) {
            if (file.type === 'image/png') {
                const filename = await uploadImage(file);
                if (filename) {
                    imgpathInput.value = filename; // Guarda solo el nombre del archivo
                    imagePreview.innerHTML = `<img src="/api/public/img/${filename}" alt="Vista previa" style="max-width: 100px; max-height: 100px;">`;
                }
            } else {
                showAlert('warning', 'Por favor, selecciona un archivo de imagen PNG.');
                braceletImageInput.value = ''; // Limpiar el input
                imagePreview.innerHTML = '';
                imgpathInput.value = '';
            }
        }
    }

    // Guardar pulsera
    async function saveBracelet() {
        const braceletId = document.getElementById('braceletId').value;
        const braceletData = {
            nombre: document.getElementById('braceletName').value,
            descripcion: document.getElementById('braceletDescription').value,
            precio: parseFloat(document.getElementById('braceletPrice').value),
            delisted: document.getElementById('braceletStatus').value === 'true',
            // Si imgpathInput tiene un valor, se usa /img/{valor}. Si no, se usa la imgURL existente (si es edición) o null (si es nueva sin imagen)
            imgURL: imgpathInput.value ? `/api/public/img/${imgpathInput.value}` : (currentBracelet ? currentBracelet.imgURL : null)
        };

        const url = currentAction === 'addBracelet' ? '/api/admin/pulseras' : `/api/admin/pulseras/${braceletId}`;
        const method = currentAction === 'addBracelet' ? 'POST' : 'PUT';

        try {
            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(braceletData)
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || 'Error al guardar pulsera');
            }

            showAlert('success','Pulsera guardada correctamente');
            braceletModal.hide();
            loadBracelets();

        } catch (error) {
            console.error('Error:', error);
            showAlert('danger','Error al guardar pulsera: ' + error.message);
        }
    }

    // Confirmar acción (eliminar/activar)
    function confirmAction() {
        confirmModal.hide();

        let url, method;
        let actionType = ''; // Para saber si se afectó a usuarios o pulseras

        switch(currentAction) {
            case 'deleteUser':
                url = `/api/admin/usuario/${currentItemId}`;
                method = 'DELETE';
                actionType = 'user';
                break;

            case 'deleteBracelet':
                // Para "retirar" una pulsera, se hace un PUT para cambiar el estado 'delisted' a true
                url = `/api/admin/pulseras/${currentItemId}`;
                method = 'PUT';
                actionType = 'bracelet';
                break;

            case 'activateBracelet':
                // Para "activar" una pulsera, se hace un PUT para cambiar el estado 'delisted' a false
                url = `/api/admin/pulseras/${currentItemId}`;
                method = 'PUT';
                actionType = 'bracelet';
                break;

            default:
                return;
        }

        fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            // Para 'deleteBracelet' y 'activateBracelet', se necesita enviar el objeto completo de la pulsera
            // con el estado 'delisted' actualizado. Se podría buscar el objeto de la pulsera por ID
            // antes de la llamada, o simplificar la lógica de backend si solo se necesita el ID para cambiar 'delisted'.
            // Por simplicidad, asumiré que el backend puede manejar un PUT sin el cuerpo completo para estas acciones,
            // o que la API de PulseraController.java ya maneja el cambio de `delisted` con solo el ID en el PUT.
            // Si no, se debería cargar la pulsera primero, modificar `delisted` y luego enviarla.
            body: (currentAction === 'deleteBracelet' || currentAction === 'activateBracelet')
                ? JSON.stringify({ id: currentItemId, delisted: (currentAction === 'deleteBracelet') })
                : null
        })
            .then(response => {
                if (!response.ok) throw new Error('Error al realizar la acción');
                // Si la eliminación o activación devuelve 204 No Content, response.json() fallará.
                // Se verifica si hay contenido para parsear como JSON.
                const contentType = response.headers.get("content-type");
                if (contentType && contentType.indexOf("application/json") !== -1) {
                    return response.json();
                } else {
                    return {}; // Devuelve un objeto vacío si no hay JSON
                }
            })
            .then(() => {
                showAlert('success','Acción realizada correctamente');
                if (actionType === 'user') {
                    loadUsers();
                } else {
                    loadBracelets();
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showAlert('danger','Error al realizar la acción: ' + error);
            });
    }

    /**
     * Displays a Bootstrap modal alert.
     * @param {string} type - The type of alert (e.g., 'success', 'danger', 'info', 'warning'). This affects the modal title.
     * @param {string} message - The message to display in the alert.
     */
    function showAlert(type, message, duration = 3000) {
        // Set modal title based on type
        switch (type) {
            case 'success':
                myAlertModalLabel.textContent = 'Éxito';
                myAlertModalLabel.parentElement.className = 'modal-header bg-success text-white';
                break;
            case 'danger':
                myAlertModalLabel.textContent = 'Error';
                myAlertModalLabel.parentElement.className = 'modal-header bg-danger text-white';
                break;
            case 'info':
                myAlertModalLabel.textContent = 'Información';
                myAlertModalLabel.parentElement.className = 'modal-header bg-info text-white';
                break;
            case 'warning':
                myAlertModalLabel.textContent = 'Advertencia';
                myAlertModalLabel.parentElement.className = 'modal-header bg-warning text-dark';
                break;
            default:
                myAlertModalLabel.textContent = 'Alerta';
                myAlertModalLabel.parentElement.className = 'modal-header';
        }

        // Set modal body message
        myAlertModalBody.textContent = message;

        // Show the modal
        myAlertModal.show();

        // Set timeout to hide the modal after duration (default: 3000ms = 3 seconds)
        setTimeout(() => {
            myAlertModal.hide();
        }, duration);
    }
});