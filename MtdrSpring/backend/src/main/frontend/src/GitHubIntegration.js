/*
** Oracle Todo Application - GitHub Integration Component
**
** Copyright (c) 2025, Oracle and/or its affiliates.
** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
*/

import React, { useState, useEffect } from 'react';
import { Button, TableBody, CircularProgress, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Snackbar, Alert } from '@mui/material';
import Moment from 'react-moment';
import { GITHUB_CREATE_BRANCH, GITHUB_GET_BRANCHES } from './API';

function GitHubIntegration({ todos }) {
  // Estados para gestionar los datos de GitHub
  const [branches, setBranches] = useState([]);
  const [isLoading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  // Estados para diálogos
  const [openCreateBranchDialog, setOpenCreateBranchDialog] = useState(false);
  const [selectedTask, setSelectedTask] = useState(null);
  const [branchName, setBranchName] = useState('');
  
  // Estados para mensajes de retroalimentación
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'info' });
  const [debugInfo, setDebugInfo] = useState('');
  
  // Parámetros GitHub para la API
  const owner = 'antoineganem';
  const repo = 'testing';
  
  // Cargar ramas del repositorio al montar el componente
  useEffect(() => {
    fetchBranches();
  }, []);
  
  // Función para obtener las ramas del repositorio
  const fetchBranches = async () => {
    setLoading(true);
    setError(null);
    setDebugInfo('Fetching branches...');
    
    try {
      const url = `${GITHUB_GET_BRANCHES}?owner=${owner}&repo=${repo}`;
      setDebugInfo(prev => `${prev}\nRequest URL: ${url}`);
      
      const response = await fetch(url);
      
      setDebugInfo(prev => `${prev}\nResponse status: ${response.status} ${response.statusText}`);
      
      if (!response.ok) {
        throw new Error(`Error fetching branches: ${response.status} ${response.statusText}`);
      }
      
      const responseText = await response.text();
      setDebugInfo(prev => `${prev}\nResponse body: ${responseText.substring(0, 200)}...`);
      
      // Intentar parsear el JSON
      let data;
      try {
        data = JSON.parse(responseText);
      } catch (parseError) {
        setDebugInfo(prev => `${prev}\nError parsing JSON: ${parseError.message}`);
        throw new Error('Invalid JSON response from server');
      }
      
      setDebugInfo(prev => `${prev}\nBranches fetched: ${data.length || 0}`);
      setBranches(data);
    } catch (err) {
      console.error('Error fetching branches:', err);
      setError(err);
      setDebugInfo(prev => `${prev}\nError: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };
  
  // Funciones para manejar acciones
  const handleCreateBranch = (taskId) => {
    const task = todos.find(todo => todo.id === taskId);
    if (!task) return;
    
    setSelectedTask(task);
    // Crear un nombre de rama basado en la descripción de la tarea
    const cleanDescription = task.description
      .toLowerCase()
      .replace(/[^\w\s-]/g, '') // Eliminar caracteres especiales
      .replace(/\s+/g, '-')     // Reemplazar espacios con guiones
      .slice(0, 30);            // Limitar longitud
      
    // Añadir timestamp para evitar duplicados
    const timestamp = new Date().getTime().toString().slice(-6);
    const uniqueBranchName = `task-${taskId}-${cleanDescription}-${timestamp}`;
    
    setBranchName(uniqueBranchName);
    setOpenCreateBranchDialog(true);
  };
  
  const confirmCreateBranch = async () => {
    if (!branchName || !selectedTask) return;
    
    setLoading(true);
    setError(null);
    
    // Mostrar exactamente qué estamos enviando para depuración
    console.log("Branch name being sent:", branchName);
    
    const requestData = {
      owner: owner,
      repo: repo,
      newBranchName: branchName,  // Este es el campo que espera el backend
      baseBranch: "main", 
      taskId: selectedTask.id
    };
    
    // Debug info para verificar los datos de la solicitud
    setDebugInfo(`Creating branch: ${branchName}\nRequest data: ${JSON.stringify(requestData, null, 2)}`);
    console.log('Sending request to create branch:', requestData);
    console.log('Endpoint URL:', GITHUB_CREATE_BRANCH);
    
    try {
      const response = await fetch(GITHUB_CREATE_BRANCH, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
      });
      
      // Capturar la respuesta completa para depuración
      const responseText = await response.text();
      
      setDebugInfo(prev => `${prev}\nResponse status: ${response.status} ${response.statusText}`);
      setDebugInfo(prev => `${prev}\nResponse body: ${responseText}`);
      
      console.log('Response from server:', response.status, response.statusText);
      console.log('Response body:', responseText);
      
      // Intentar parsear la respuesta como JSON si es posible
      let jsonResponse = null;
      try {
        if (responseText && responseText.trim().startsWith('{')) {
          jsonResponse = JSON.parse(responseText);
          console.log('Parsed JSON response:', jsonResponse);
          setDebugInfo(prev => `${prev}\nParsed response: ${JSON.stringify(jsonResponse, null, 2)}`);
        }
      } catch (parseError) {
        console.log('Response is not JSON:', responseText);
      }
      
      // Manejar error 422 (branch ya existe) específicamente
      if (response.status === 422 && jsonResponse?.message === "Reference already exists") {
        // Crear un nuevo nombre con timestamp
        const timestamp = new Date().getTime().toString().slice(-6);
        const newBranchName = `${branchName}-${timestamp}`;
        
        setDebugInfo(prev => `${prev}\nBranch already exists. Retrying with: ${newBranchName}`);
        
        // Reintentar con el nuevo nombre
        const retryData = {
          ...requestData,
          newBranchName: newBranchName  // Este es el campo que espera el backend
        };
        
        const retryResponse = await fetch(GITHUB_CREATE_BRANCH, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(retryData)
        });
        
        const retryText = await retryResponse.text();
        setDebugInfo(prev => `${prev}\nRetry response: ${retryResponse.status} ${retryResponse.statusText}`);
        setDebugInfo(prev => `${prev}\nRetry body: ${retryText}`);
        
        if (!retryResponse.ok) {
          throw new Error(`Error creating branch on retry: ${retryResponse.status} - ${retryText}`);
        }
        
        // Actualizar el nombre de la rama si el reintento fue exitoso
        setBranchName(newBranchName);
        
        // Notificar éxito
        setNotification({
          open: true,
          message: `Branch ${newBranchName} created successfully (after retry)!`,
          severity: 'success'
        });
        
      } else if (!response.ok) {
        const errorMessage = jsonResponse?.message || responseText || response.statusText;
        throw new Error(`Error creating branch: ${response.status} - ${errorMessage}`);
      } else {
        // Notificar éxito
        setNotification({
          open: true,
          message: `Branch ${branchName} created successfully!`,
          severity: 'success'
        });
      }
      
      // Refrescar la lista de ramas
      await fetchBranches();
      
      // Cerrar el diálogo
      setOpenCreateBranchDialog(false);
      setSelectedTask(null);
      setBranchName('');
      
    } catch (err) {
      console.error('Error creating branch:', err);
      setDebugInfo(prev => `${prev}\nError: ${err.message}`);
      
      // Mostrar error
      setError(err);
      setNotification({
        open: true,
        message: `Failed to create branch: ${err.message}`,
        severity: 'error'
      });
    } finally {
      setLoading(false);
    }
  };
  
  // Comprueba si una tarea ya tiene una rama asociada
  const taskHasBranch = (taskId) => {
    if (!branches || !branches.length) return false;
    
    return branches.some(branch => {
      // Verificar si el nombre de la rama contiene el ID de la tarea
      return branch.name.includes(`task-${taskId}-`);
    });
  };
  
  // Cerrar notificación
  const handleCloseNotification = () => {
    setNotification({ ...notification, open: false });
  };
  
  // Para mostrar/ocultar información de depuración
  const [showDebug, setShowDebug] = useState(true); // Activado por defecto para facilitar depuración
  
  // Renderizado de los componentes
  return (
    <div>
      <h1>GITHUB INTEGRATION</h1>
      
      {/* Botón para mostrar/ocultar información de depuración */}
      <div style={{ textAlign: 'right', marginBottom: '15px' }}>
        <Button 
          variant="outlined" 
          size="small"
          onClick={() => setShowDebug(!showDebug)}
          style={{ 
            backgroundColor: 'rgba(255, 255, 255, 0.07)',
            color: 'rgba(255, 255, 255, 0.7)',
            border: '1px solid rgba(255, 255, 255, 0.15)'
          }}
        >
          {showDebug ? 'Hide Debug Info' : 'Show Debug Info'}
        </Button>
      </div>
      
      {showDebug && debugInfo && (
        <div className="debug-info">
          <h3>Debug Information</h3>
          <pre>{debugInfo}</pre>
        </div>
      )}
      
      {error && (
        <div className="error-message">
          <p>Error: {error.message}</p>
        </div>
      )}
      
      {isLoading ? (
        <div className="loading-container">
          <div className="loading-spinner"></div>
        </div>
      ) : (
        <div id="maincontent">
          {/* Active Tasks for Branch Creation */}
          <div className="task-section-header">
            <h2>Tasks Available for Branch Creation <span className="task-count">
              {todos.filter(todo => !todo.done && !taskHasBranch(todo.id)).length}
            </span></h2>
          </div>
          
          <table className="itemlist">
            <TableBody>
            {todos.filter(todo => !todo.done && !taskHasBranch(todo.id)).length === 0 ? (
              <tr>
                <td colSpan="3" className="empty-message">
                  No tasks available for branch creation.
                </td>
              </tr>
            ) : (
              todos.filter(todo => !todo.done && !taskHasBranch(todo.id)).map(todo => (
                <tr key={`todo-${todo.id}`}>
                  <td className="description">{todo.description}</td>
                  <td className="date">{todo.createdAt && <Moment format="MMM Do HH:mm">{todo.createdAt}</Moment>}</td>
                  <td>
                    <Button 
                      variant="contained" 
                      className="AddButton" 
                      onClick={() => handleCreateBranch(todo.id)} 
                      size="small"
                    >
                      Create Branch
                    </Button>
                  </td>
                </tr>
              ))
            )}
            </TableBody>
          </table>
          
          {/* Branches Section */}
          <div className="task-section-header">
            <h2>Repository Branches <span className="task-count">{branches.length}</span></h2>
            <Button 
              variant="outlined" 
              size="small" 
              onClick={() => fetchBranches()}
              style={{ marginLeft: 'auto' }}
            >
              Refresh
            </Button>
          </div>
          
          <table className="itemlist">
            <TableBody>
            {branches.length === 0 ? (
              <tr>
                <td colSpan="3" className="empty-message">
                  No branches found in repository.
                </td>
              </tr>
            ) : (
              branches.map((branch, index) => (
                <tr key={`branch-${index}`}>
                  <td className="description">{branch.name}</td>
                  <td className="date">
                    {branch.commit && branch.commit.sha && 
                      <span className="commit-sha">#{branch.commit.sha.substring(0, 7)}</span>
                    }
                  </td>
                  <td>
                    <a 
                      href={`https://github.com/${owner}/${repo}/tree/${branch.name}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="github-link"
                    >
                      View on GitHub
                    </a>
                  </td>
                </tr>
              ))
            )}
            </TableBody>
          </table>
        </div>
      )}
      
      {/* Diálogo para crear una rama */}
      <Dialog open={openCreateBranchDialog} onClose={() => setOpenCreateBranchDialog(false)}>
        <DialogTitle>Create Branch for {owner}/{repo}</DialogTitle>
        <DialogContent>
          <p>Creating branch for task: {selectedTask?.description}</p>
          <TextField
            autoFocus
            margin="dense"
            id="branch-name"
            label="Branch Name"
            type="text"
            fullWidth
            variant="outlined"
            value={branchName}
            onChange={(e) => setBranchName(e.target.value)}
          />
          <p className="dialog-help-text">
            The branch will be created from the repository's main branch.
            A timestamp has been added to the branch name to avoid conflicts.
          </p>
          
          {/* Mostrar información del endpoint para depuración */}
          <div className="debug-endpoint-info">
            <h4>Branch Creation Information</h4>
            <p>Owner: {owner}</p>
            <p>Repo: {repo}</p>
            <p>Base Branch: main</p>
            <p>New Branch Name: {branchName}</p>
            <p>Task ID: {selectedTask?.id}</p>
            <p>Will be sent as: {"{"} owner: '{owner}', repo: '{repo}', newBranchName: '{branchName}', baseBranch: 'main', taskId: {selectedTask?.id} {"}"}</p>
          </div>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenCreateBranchDialog(false)} className="DeleteButton">Cancel</Button>
          <Button onClick={confirmCreateBranch} className="DoneButton" disabled={isLoading || !branchName}>
            {isLoading ? 'Creating...' : 'Create Branch'}
          </Button>
        </DialogActions>
      </Dialog>
      
      {/* Notificaciones */}
      <Snackbar 
        open={notification.open} 
        autoHideDuration={6000} 
        onClose={handleCloseNotification}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert 
          onClose={handleCloseNotification} 
          severity={notification.severity}
          sx={{ width: '100%' }}
        >
          {notification.message}
        </Alert>
      </Snackbar>
      
      {/* Estilos adicionales para depuración */}
      <style jsx>{`
        .debug-info {
          background-color: rgba(0, 0, 0, 0.7);
          border: 1px solid rgba(255, 255, 255, 0.1);
          border-radius: 4px;
          padding: 10px;
          margin-bottom: 20px;
          overflow: auto;
          max-height: 200px;
        }
        
        .debug-info h3 {
          margin-top: 0;
          color: #C74634;
          font-size: 16px;
        }
        
        .debug-info pre {
          color: rgba(255, 255, 255, 0.7);
          font-family: monospace;
          font-size: 12px;
          white-space: pre-wrap;
          word-break: break-all;
        }
        
        .debug-endpoint-info {
          margin-top: 15px;
          padding: 10px;
          background-color: rgba(0, 0, 0, 0.5);
          border-radius: 4px;
        }
        
        .debug-endpoint-info h4 {
          margin-top: 0;
          color: #C74634;
          font-size: 14px;
        }
        
        .debug-endpoint-info p {
          margin: 5px 0;
          font-size: 12px;
          font-family: monospace;
        }
      `}</style>
    </div>
  );
}

export default GitHubIntegration;