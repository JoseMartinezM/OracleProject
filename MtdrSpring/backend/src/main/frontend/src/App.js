/*
** Oracle Todo Application - Main App Component
**
** Copyright (c) 2025, Oracle and/or its affiliates.
** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
*/

import React, { useState, useEffect } from 'react';
import NewItem from './NewItem';
import GitHubIntegration from './GitHubIntegration';
import { API_LIST } from './API';
import { Button, TableBody, Modal, Box, Typography, IconButton } from '@mui/material';
import Moment from 'react-moment';
import CloseIcon from '@mui/icons-material/Close';
import './index.css';
import { TextField, Dialog, DialogTitle, DialogContent, DialogActions } from '@mui/material';

function App() {
    // Estados de la aplicación
    const [isLoading, setLoading] = useState(false);
    const [isInserting, setInserting] = useState(false);
    const [items, setItems] = useState([]);
    const [error, setError] = useState();
    
    // Estado para la pestaña activa
    const [activeTab, setActiveTab] = useState('tasks'); // 'tasks' o 'github'
    
    // Estado para filtros
    const [priorityFilter, setPriorityFilter] = useState('All');

    // Estado para el modal
    const [modalOpen, setModalOpen] = useState(false);
    const [selectedTask, setSelectedTask] = useState(null);

    // Estados pata la autenticacion
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [currentUser, setCurrentUser] = useState({});
    const [loginOpen, setLoginOpen] = useState(true);
    const [loginData, setLoginData] = useState({
        username: '',
        password: ''
    });
    const [loginError, setLoginError] = useState('');

    // Login handler
    const handleLogin = async () => {
      try {
          setLoginError('');
          const response = await fetch('/api/users/username/' + loginData.username);
          if (response.ok) {
              const user = await response.json();
              if (user.password === loginData.password) {
                  setIsAuthenticated(true);
                  setCurrentUser({
                      ID: user.ID,
                      username: user.username,
                      role: user.role,
                      name: user.name
                  });
                  setLoginOpen(false);
              } else {
                  setLoginError('Invalid password');
              }
          } else {
              setLoginError('User not found');
          }
      } catch (error) {
          setLoginError('Login failed');
          console.error('Login error:', error);
      }
    };

  // Logout handler
  const handleLogout = () => {
      setIsAuthenticated(false);
      setCurrentUser({});
      setLoginOpen(true);
      setLoginData({ username: '', password: '' });
  };

  // Update login form data
  const handleLoginChange = (e) => {
      const { name, value } = e.target;
      setLoginData(prev => ({
          ...prev,
          [name]: value
      }));
  };

    // Función para abrir el modal con la tarea seleccionada
    const openTaskModal = (task) => {
        setSelectedTask(task);
        setModalOpen(true);
    };

    // Función para cerrar el modal
    const closeTaskModal = () => {
        setModalOpen(false);
    };

    function deleteItem(deleteId) {
      // First check if the current user is the creator or a manager
      const taskToDelete = items.find(item => item.id === deleteId);
      if (currentUser.role !== 'Manager' && taskToDelete.createdBy !== currentUser.ID) {
          setError(new Error('Only managers or task creators can delete tasks'));
          return;
      }
      fetch(`${API_LIST}/${deleteId}`, {
        method: 'DELETE',
      })
      .then(response => {
        if (response.ok) {
          return response;
        } else {
          throw new Error('Something went wrong ...');
        }
      })
      .then(
        (result) => {
          const remainingItems = items.filter(item => item.id !== deleteId);
          setItems(remainingItems);
          // Si el modal está abierto con esta tarea, cerrarlo
          if (selectedTask && selectedTask.id === deleteId) {
            closeTaskModal();
          }
        },
        (error) => {
          setError(error);
        }
      );
    }
    
    function toggleDone(event, id, description, done) {
      event.stopPropagation(); // Evitar que se abra el modal
      modifyItem(id, description, done).then(
        (result) => { reloadOneItem(id); },
        (error) => { setError(error); }
      );
    }
    
    function reloadOneItem(id){
      fetch(`${API_LIST}/${id}`)
        .then(response => {
          if (response.ok) {
            return response.json();
          } else {
            throw new Error('Something went wrong ...');
          }
        })
        .then(
          (result) => {
            const updatedItems = items.map(
              x => (x.id === id ? {
                 ...x,
                 'description': result.description,
                 'done': result.done,
                 'status': result.status,
                 'priority': result.priority,
                 'steps': result.steps,
                 'assignedTo': result.assignedTo,
                 'createdBy': result.createdBy,
                 'isArchived': result.isArchived,
                 'creation_ts': result.creation_ts
                } : x));
            setItems(updatedItems);
            
            // Actualizar la tarea seleccionada si está abierta en el modal
            if (selectedTask && selectedTask.id === id) {
              setSelectedTask(updatedItems.find(item => item.id === id));
            }
          },
          (error) => {
            setError(error);
          });
    }
    
    function modifyItem(id, description, done) {
      // Encuentra el item actual para preservar otros campos
      const currentItem = items.find(item => item.id === id);
      
      // Actualiza solo los campos necesarios
      var data = {
        "description": description, 
        "done": done,
        "status": done ? "Completed" : (currentItem.status === "Completed" ? "In Progress" : currentItem.status)
      };
      
      return fetch(`${API_LIST}/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
      })
      .then(response => {
        if (response.ok) {
          return response;
        } else {
          throw new Error('Something went wrong ...');
        }
      });
    }
    
    function updateStatus(event, id, newStatus) {
      event.stopPropagation(); // Evitar que se abra el modal
      fetch(`${API_LIST}/${id}/status`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ status: newStatus })
      })
      .then(response => {
        if (response.ok) {
          return response.json();
        } else {
          throw new Error('Failed to update status');
        }
      })
      .then(
        (result) => {
          reloadOneItem(id);
        },
        (error) => {
          setError(error);
        }
      );
    }

    function updateTaskFromModal(id, updatedTask) {
      fetch(`${API_LIST}/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(updatedTask)
      })
      .then(response => {
        if (response.ok) {
          return response.json();
        } else {
          throw new Error('Failed to update task');
        }
      })
      .then(
        (result) => {
          reloadOneItem(id);
          closeTaskModal();
        },
        (error) => {
          setError(error);
        }
      );
    }
    
    useEffect(() => {
      setLoading(true);
      fetch(API_LIST)
        .then(response => {
          if (response.ok) {
            return response.json();
          } else {
            throw new Error('Something went wrong ...');
          }
        })
        .then(
          (result) => {
            setLoading(false);
            // Adaptando al nuevo formato de datos
            setItems(result.map(item => ({
              id: item.id,
              description: item.description,
              done: item.done,
              createdAt: item.creation_ts,
              status: item.status || 'Pending',
              priority: item.priority || 'Medium',
              steps: item.steps || '',
              assignedTo: item.assignedTo,
              createdBy: item.createdBy,
              isArchived: item.isArchived
            })));
          },
          (error) => {
            setLoading(false);
            setError(error);
          });
    }, []);
    
    // Example modification to addItem function
function addItem(text, steps, priority) {
  if (!currentUser?.ID) {
    setError(new Error('User not authenticated'));
    return;
  }
  setInserting(true);
  
  const currentTimestamp = new Date().toISOString();
  
  var data = {
      description: text,
      done: false,
      status: "Pending",
      priority: priority,
      steps: steps || '',
      creation_ts: currentTimestamp,
      createdBy: currentUser.ID // Add the current user's ID
  };
  
  fetch(API_LIST, {
      method: 'POST',
      headers: {
          'Content-Type': 'application/json'
      },
      body: JSON.stringify(data),
  })
  .then((response) => {
      if (response.ok) {
          return response;
      } else {
          throw new Error('Something went wrong ...');
      }
  })
  .then(
      (result) => {
          var id = result.headers.get('location');
          
          var newItem = {
              id: id, 
              description: text, 
              done: false,
              status: "Pending",
              priority: priority,
              steps: steps || '',
              createdAt: currentTimestamp,
              assignedTo: null,
              createdBy: currentUser.ID, // Add the current user's ID
              isArchived: 0
          };
          
          setItems([newItem, ...items]);
          setInserting(false);
      },
      (error) => {
          setInserting(false);
          setError(error);
      }
  );
}
    
    // Filtrado por prioridad
    const filteredItems = items.filter(item => {
      let priorityMatch = priorityFilter === 'All' || item.priority === priorityFilter;
      return priorityMatch;
    });
    
    // Agrupar por estado en lugar de por "done"
    const pendingItems = filteredItems.filter(item => item.status === 'Pending');
    const inProgressItems = filteredItems.filter(item => item.status === 'In Progress');
    const inReviewItems = filteredItems.filter(item => item.status === 'In Review');
    const completedItems = filteredItems.filter(item => item.status === 'Completed');
    
    const priorityOptions = ['All', 'Low', 'Medium', 'High', 'Critical'];
    
    // Función para renderizar las filas de tareas
    const renderTaskRows = (tasks) => {
      if (tasks.length === 0) {
        return (
          <tr>
            <td colSpan="4" className="empty-message">
              No tasks in this section.
            </td>
          </tr>
        );
      }
      
      return tasks.map(item => (
        <tr key={item.id} onClick={() => openTaskModal(item)} className="task-row">
          <td className="description">
            <div>{item.description}</div>
          </td>
          <td className="status-priority">
            <span className={`priority priority-${item.priority?.toLowerCase()}`}>
              {item.priority}
            </span>
          </td>
          <td className="date">
            {item.createdAt && <Moment format="MMM Do HH:mm">{item.createdAt}</Moment>}
          </td>
          <td onClick={(e) => e.stopPropagation()}>
            {item.status !== 'Completed' ? (
              <Button 
                variant="contained" 
                className="DoneButton" 
                onClick={(event) => toggleDone(event, item.id, item.description, true)} 
                size="small"
              >
                Done
              </Button>
            ) : (
              <Button 
                variant="contained" 
                className="DeleteButton" 
                onClick={() => deleteItem(item.id)} 
                size="small"
              >
                Delete
              </Button>
            )}
          </td>
        </tr>
      ));
    };
    
    // Estilos para el modal
    const modalStyle = {
      position: 'absolute',
      top: '50%',
      left: '50%',
      transform: 'translate(-50%, -50%)',
      width: '80%',
      maxWidth: 600,
      maxHeight: '80vh',
      overflow: 'auto',
      bgcolor: '#312D2A', // Color de fondo Oracle Dark
      boxShadow: 24,
      p: 4,
      borderRadius: '8px',
      color: 'white',
      border: '1px solid rgba(255, 255, 255, 0.1)'
    };

    return (
      <div className="App">
        {/* Login Dialog - shown when not authenticated */}
        {!isAuthenticated && (
          <Dialog open={loginOpen} onClose={() => setLoginOpen(false)}>
            <DialogTitle>Login</DialogTitle>
            <DialogContent>
              <TextField
                autoFocus
                margin="dense"
                name="username"
                type="text"
                label="Username"
                fullWidth
                variant='standard'
                value={loginData.username}
                onChange={handleLoginChange}
              />
              <TextField
                margin="dense"
                name="password"
                label="Password"
                type="password"
                fullWidth
                variant="standard"
                value={loginData.password}
                onChange={handleLoginChange}
              />
              {loginError && <p className="error-message">{loginError}</p>}
              </DialogContent>
              <DialogActions>
                <Button onClick={handleLogin}>Login</Button>
              </DialogActions>
            </Dialog>
        )}

        {/*Only show app content if authenticated*/}
        <>
          {/* Add logout button at the top */}
          <div style={{ position: 'absolute', top: '10px', right: '10px' }}>
            <Button
              variant="contained"
              color="secondary"
              onClick={handleLogout}
            >
              Logout ({currentUser?.username || 'User'})
            </Button>
          </div>

          {/* Oracle Logo (tamaño aumentado) */}
          <img 
            src="https://imgs.search.brave.com/VP0I6z3w18_vEzuRoDlY0arRjFf9OdUsX3928ysRXmE/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9sb2dv/ZG93bmxvYWQub3Jn/L3dwLWNvbnRlbnQv/dXBsb2Fkcy8yMDE0/LzA0L29yYWNsZS1s/b2dvLTAucG5n" 
            alt="Oracle Logo" 
            className="oracle-logo" 
            style={{ height: "180px" }} // Hacer el logo más grande
          />
        </>

        <h1>TODO LIST</h1>
        
        {/* Pestañas de navegación */}
        <div className="app-tabs">
          <div 
            className={`app-tab ${activeTab === 'tasks' ? 'active' : ''}`}
            onClick={() => setActiveTab('tasks')}
          >
            Tasks
          </div>
          <div 
            className={`app-tab ${activeTab === 'github' ? 'active' : ''}`}
            onClick={() => setActiveTab('github')}
          >
            GitHub Integration
          </div>
        </div>
        
        {/* Contenido condicional según la pestaña activa */}
        {activeTab === 'tasks' ? (
          <>
            <NewItem addItem={addItem} isInserting={isInserting}/>
            
            { error &&
              <div className="error-message">
                <p>Error: {error.message}</p>
              </div>
            }
            
            {/* Filtros (solo por prioridad) */}
            <div className="filters">
              <div className="filter-group">
                <label>Priority:</label>
                <select 
                  value={priorityFilter} 
                  onChange={(e) => setPriorityFilter(e.target.value)}
                >
                  {priorityOptions.map(option => (
                    <option key={option} value={option}>{option}</option>
                  ))}
                </select>
              </div>
            </div>
            
            { isLoading ? (
              <div className="loading-container">
                <div className="loading-spinner"></div>
              </div>
            ) : (
              <div id="maincontent">
                {/* Pending Tasks Section */}
                <div className="task-section-header">
                  <h2>Pending <span className="task-count">{pendingItems.length}</span></h2>
                </div>
                
                <table className="itemlist">
                  <TableBody>
                    {renderTaskRows(pendingItems)}
                  </TableBody>
                </table>
                
                {/* In Progress Tasks Section */}
                <div className="task-section-header">
                  <h2>In Progress <span className="task-count">{inProgressItems.length}</span></h2>
                </div>
                
                <table className="itemlist">
                  <TableBody>
                    {renderTaskRows(inProgressItems)}
                  </TableBody>
                </table>
                
                {/* In Review Tasks Section */}
                <div className="task-section-header">
                  <h2>In Review <span className="task-count">{inReviewItems.length}</span></h2>
                </div>
                
                <table className="itemlist">
                  <TableBody>
                    {renderTaskRows(inReviewItems)}
                  </TableBody>
                </table>
                
                {/* Completed Tasks Section */}
                <div className="task-section-header">
                  <h2>Completed <span className="task-count">{completedItems.length}</span></h2>
                </div>
                
                <table className="itemlist">
                  <TableBody>
                    {renderTaskRows(completedItems)}
                  </TableBody>
                </table>
              </div>
            )}
          </>
        ) : (
          <GitHubIntegration todos={items} />
        )}

        {/* Modal para detalles de tarea */}
        <Modal
          open={modalOpen}
          onClose={closeTaskModal}
          aria-labelledby="task-details-modal"
          aria-describedby="detailed view of the selected task"
        >
          <Box sx={modalStyle}>
            {selectedTask && (
              <>
                <div className="modal-header">
                  <Typography variant="h5" component="h2" className="modal-title">
                    Task Details
                  </Typography>
                  <IconButton 
                    aria-label="close" 
                    onClick={closeTaskModal}
                    sx={{ 
                      color: 'white',
                      position: 'absolute',
                      right: 8,
                      top: 8
                    }}
                  >
                    <CloseIcon />
                  </IconButton>
                </div>

                <div className="modal-content">
                  <div className="modal-section">
                    <Typography variant="h6" className="modal-section-title">Description</Typography>
                    <Typography variant="body1" className="modal-description">
                      {selectedTask.description}
                    </Typography>
                  </div>

                  {selectedTask.steps && (
                    <div className="modal-section">
                      <Typography variant="h6" className="modal-section-title">Steps</Typography>
                      <Typography variant="body1" className="modal-steps">
                        {selectedTask.steps.split('\n').map((step, index) => (
                          <div key={index} className="step-item">
                            {step}
                          </div>
                        ))}
                      </Typography>
                    </div>
                  )}

                  <div className="modal-meta">
                    <div className="modal-meta-item">
                      <Typography variant="body2" className="modal-meta-label">Status</Typography>
                      <span className={`status status-${selectedTask.status?.toLowerCase().replace(/\s+/g, '-')}`}>
                        {selectedTask.status}
                      </span>
                    </div>

                    <div className="modal-meta-item">
                      <Typography variant="body2" className="modal-meta-label">Priority</Typography>
                      <span className={`priority priority-${selectedTask.priority?.toLowerCase()}`}>
                        {selectedTask.priority}
                      </span>
                    </div>

                    <div className="modal-meta-item">
                      <Typography variant="body2" className="modal-meta-label">Created</Typography>
                      <Typography variant="body2" className="modal-meta-value">
                        {selectedTask.createdAt && <Moment format="MMMM Do YYYY, h:mm a">{selectedTask.createdAt}</Moment>}
                      </Typography>
                    </div>

                    {selectedTask.assignedTo && (
                      <div className="modal-meta-item">
                        <Typography variant="body2" className="modal-meta-label">Assigned To</Typography>
                        <Typography variant="body2" className="modal-meta-value">
                          User ID: {selectedTask.assignedTo}
                        </Typography>
                      </div>
                    )}

                    {selectedTask.createdBy && (
                      <div className="modal-meta-item">
                        <Typography variant="body2" className="modal-meta-label">Created By</Typography>
                        <Typography variant="body2" className="modal-meta-value">
                          User ID: {selectedTask.createdBy}
                        </Typography>
                      </div>
                    )}
                  </div>

                  <div className="modal-actions">
                    {selectedTask.status !== "Completed" && (
                      <Button 
                        variant="contained" 
                        className="modal-action-button move-next"
                        onClick={(e) => {
                          let nextStatus;
                          switch(selectedTask.status) {
                            case "Pending":
                              nextStatus = "In Progress";
                              break;
                            case "In Progress":
                              nextStatus = "In Review";
                              break;
                            case "In Review":
                              nextStatus = "Completed";
                              break;
                            default:
                              nextStatus = "Completed";
                          }
                          updateStatus(e, selectedTask.id, nextStatus);
                        }}
                      >
                        Move to {
                          selectedTask.status === "Pending" ? "In Progress" :
                          selectedTask.status === "In Progress" ? "In Review" :
                          selectedTask.status === "In Review" ? "Completed" : "Next"
                        }
                      </Button>
                    )}
                    
                    {selectedTask.status === "Completed" && (
                      <Button 
                        variant="contained" 
                        className="modal-action-button delete-task"
                        onClick={() => {
                          deleteItem(selectedTask.id);
                          closeTaskModal();
                        }}
                      >
                        Delete Task
                      </Button>
                    )}
                  </div>
                </div>
              </>
            )}
          </Box>
        </Modal>
      </div>
    );
}

export default App;