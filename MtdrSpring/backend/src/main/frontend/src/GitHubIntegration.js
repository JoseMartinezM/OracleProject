/*
** Oracle Todo Application - GitHub Integration Component
**
** Copyright (c) 2025, Oracle and/or its affiliates.
** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
*/

import React, { useState, useEffect } from 'react';
import { Button, TableBody, CircularProgress, Dialog, DialogTitle, DialogContent, DialogActions, TextField } from '@mui/material';
import Moment from 'react-moment';

function GitHubIntegration({ todos }) {
  // Estados para gestionar los datos de GitHub
  const [branches, setBranches] = useState([]);
  const [pullRequests, setPullRequests] = useState([]);
  const [isLoading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  // Estados para diálogos
  const [openCreateBranchDialog, setOpenCreateBranchDialog] = useState(false);
  const [selectedTask, setSelectedTask] = useState(null);
  const [branchName, setBranchName] = useState('');
  
  // Datos de ejemplo para la demostración del frontend
  useEffect(() => {
    // Simular carga de datos
    setLoading(true);
    
    // Datos de ejemplo de ramas
    const dummyBranches = [
      { id: 1, name: 'task-123-add-login-page', taskId: 123, createdAt: new Date('2025-03-01T10:30:00'), status: 'active' },
      { id: 2, name: 'task-456-fix-css-issues', taskId: 456, createdAt: new Date('2025-03-05T14:20:00'), status: 'merged' },
      { id: 3, name: 'task-789-update-api', taskId: 789, createdAt: new Date('2025-03-08T09:15:00'), status: 'active' }
    ];
    
    // Datos de ejemplo de pull requests
    const dummyPRs = [
      { id: 101, branchId: 1, title: 'Add login page', status: 'open', createdAt: new Date('2025-03-02T11:45:00'), ciStatus: 'running' },
      { id: 102, branchId: 2, title: 'Fix CSS issues', status: 'merged', createdAt: new Date('2025-03-06T16:30:00'), ciStatus: 'success' },
      { id: 103, branchId: 3, title: 'Update API integration', status: 'open', createdAt: new Date('2025-03-09T10:20:00'), ciStatus: 'failed' }
    ];
    
    // Simular retraso de carga
    setTimeout(() => {
      setBranches(dummyBranches);
      setPullRequests(dummyPRs);
      setLoading(false);
    }, 1000);
  }, []);
  
  // Funciones para manejar acciones
  const handleCreateBranch = (taskId) => {
    const task = todos.find(todo => todo.id === taskId);
    setSelectedTask(task);
    setBranchName(`task-${taskId}-${task?.description.toLowerCase().replace(/\s+/g, '-').slice(0, 20)}`);
    setOpenCreateBranchDialog(true);
  };
  
  const confirmCreateBranch = () => {
    // Simulación de creación de rama
    const newBranch = {
      id: branches.length + 1,
      name: branchName,
      taskId: selectedTask.id,
      createdAt: new Date(),
      status: 'active'
    };
    
    setBranches([newBranch, ...branches]);
    setOpenCreateBranchDialog(false);
    
    // Reiniciar estados
    setSelectedTask(null);
    setBranchName('');
  };
  
  const handleCreatePR = (branchId) => {
    // Simulación de creación de PR
    const branch = branches.find(b => b.id === branchId);
    const task = todos.find(todo => todo.id === branch.taskId);
    
    const newPR = {
      id: pullRequests.length + 101,
      branchId: branchId,
      title: task?.description || `PR from branch ${branch.name}`,
      status: 'open',
      createdAt: new Date(),
      ciStatus: 'pending'
    };
    
    setPullRequests([newPR, ...pullRequests]);
    
    // Simular inicio de CI
    setTimeout(() => {
      setPullRequests(prs => prs.map(pr => 
        pr.id === newPR.id ? { ...pr, ciStatus: 'running' } : pr
      ));
      
      // Simular finalización de CI
      setTimeout(() => {
        const result = Math.random() > 0.3 ? 'success' : 'failed';
        setPullRequests(prs => prs.map(pr => 
          pr.id === newPR.id ? { ...pr, ciStatus: result } : pr
        ));
      }, 3000);
    }, 1000);
  };
  
  const handleMergePR = (prId) => {
    // Actualizar el PR a merged
    setPullRequests(prs => prs.map(pr => 
      pr.id === prId ? { ...pr, status: 'merged' } : pr
    ));
    
    // Actualizar la rama asociada a merged
    const pr = pullRequests.find(p => p.id === prId);
    setBranches(branches => branches.map(branch => 
      branch.id === pr.branchId ? { ...branch, status: 'merged' } : branch
    ));
  };
  
  // Renderizado de los componentes
  return (
    <div>
      <h1>GITHUB INTEGRATION</h1>
      
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
            <h2>Tasks Available for Branch Creation <span className="task-count">{todos.filter(todo => !todo.done && !branches.some(b => b.taskId === todo.id)).length}</span></h2>
          </div>
          
          <table className="itemlist">
            <TableBody>
            {todos.filter(todo => !todo.done && !branches.some(b => b.taskId === todo.id)).length === 0 ? (
              <tr>
                <td colSpan="3" className="empty-message">
                  No tasks available for branch creation.
                </td>
              </tr>
            ) : (
              todos.filter(todo => !todo.done && !branches.some(b => b.taskId === todo.id)).map(todo => (
                <tr key={`todo-${todo.id}`}>
                  <td className="description">{todo.description}</td>
                  <td className="date"><Moment format="MMM Do HH:mm">{todo.createdAt}</Moment></td>
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
            <h2>Active Branches <span className="task-count">{branches.filter(b => b.status === 'active').length}</span></h2>
          </div>
          
          <table className="itemlist">
            <TableBody>
            {branches.filter(b => b.status === 'active').length === 0 ? (
              <tr>
                <td colSpan="4" className="empty-message">
                  No active branches.
                </td>
              </tr>
            ) : (
              branches.filter(b => b.status === 'active').map(branch => {
                const hasPR = pullRequests.some(pr => pr.branchId === branch.id);
                return (
                  <tr key={`branch-${branch.id}`}>
                    <td className="description">{branch.name}</td>
                    <td className="date"><Moment format="MMM Do HH:mm">{branch.createdAt}</Moment></td>
                    <td>
                      {!hasPR && (
                        <Button 
                          variant="contained" 
                          className="DoneButton" 
                          onClick={() => handleCreatePR(branch.id)} 
                          size="small"
                        >
                          Create PR
                        </Button>
                      )}
                      {hasPR && (
                        <span className="status-badge">Has PR</span>
                      )}
                    </td>
                  </tr>
                );
              })
            )}
            </TableBody>
          </table>
          
          {/* Pull Requests Section */}
          <div className="task-section-header">
            <h2>Pull Requests <span className="task-count">{pullRequests.filter(pr => pr.status === 'open').length}</span></h2>
          </div>
          
          <table className="itemlist">
            <TableBody>
            {pullRequests.filter(pr => pr.status === 'open').length === 0 ? (
              <tr>
                <td colSpan="5" className="empty-message">
                  No open pull requests.
                </td>
              </tr>
            ) : (
              pullRequests.filter(pr => pr.status === 'open').map(pr => (
                <tr key={`pr-${pr.id}`} className={pr.ciStatus === 'failed' ? 'ci-failed' : ''}>
                  <td className="description">{pr.title}</td>
                  <td className="date"><Moment format="MMM Do HH:mm">{pr.createdAt}</Moment></td>
                  <td>
                    <span className={`ci-status ci-${pr.ciStatus}`}>
                      CI: {pr.ciStatus === 'running' ? 'Running...' : pr.ciStatus}
                    </span>
                  </td>
                  <td>
                    {pr.ciStatus === 'success' && (
                      <Button 
                        variant="contained" 
                        className="DoneButton" 
                        onClick={() => handleMergePR(pr.id)} 
                        size="small"
                      >
                        Merge PR
                      </Button>
                    )}
                    {pr.ciStatus === 'failed' && (
                      <Button 
                        variant="contained" 
                        className="DeleteButton" 
                        disabled 
                        size="small"
                      >
                        CI Failed
                      </Button>
                    )}
                  </td>
                </tr>
              ))
            )}
            </TableBody>
          </table>
          
          {/* Merged PRs Section */}
          <div className="task-section-header">
            <h2>Merged Pull Requests <span className="task-count">{pullRequests.filter(pr => pr.status === 'merged').length}</span></h2>
          </div>
          
          <table className="itemlist">
            <TableBody>
            {pullRequests.filter(pr => pr.status === 'merged').length === 0 ? (
              <tr>
                <td colSpan="3" className="empty-message">
                  No merged pull requests.
                </td>
              </tr>
            ) : (
              pullRequests.filter(pr => pr.status === 'merged').map(pr => (
                <tr key={`merged-pr-${pr.id}`}>
                  <td className="description">{pr.title}</td>
                  <td className="date"><Moment format="MMM Do HH:mm">{pr.createdAt}</Moment></td>
                  <td>
                    <span className="status-badge merged">Merged to Production</span>
                  </td>
                </tr>
              ))
            )}
            </TableBody>
          </table>
        </div>
      )}
      
      {/* Diálogos */}
      <Dialog open={openCreateBranchDialog} onClose={() => setOpenCreateBranchDialog(false)}>
        <DialogTitle>Create Branch</DialogTitle>
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
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenCreateBranchDialog(false)} className="DeleteButton">Cancel</Button>
          <Button onClick={confirmCreateBranch} className="DoneButton">Create</Button>
        </DialogActions>
      </Dialog>
    </div>
  );
}

export default GitHubIntegration;