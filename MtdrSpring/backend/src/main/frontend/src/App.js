/*
** Oracle Todo Application - Main App Component with GitHub Integration
**
** Copyright (c) 2025, Oracle and/or its affiliates.
** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
*/

import React, { useState, useEffect } from 'react';
import NewItem from './NewItem';
import GitHubIntegration from './GitHubIntegration';
import API_LIST from './API';
import { Button, TableBody, CircularProgress } from '@mui/material';
import Moment from 'react-moment';
import './github-integration.css';

function App() {
    // Estado para controlar la pestaña activa
    const [activeTab, setActiveTab] = useState('tasks'); // 'tasks' o 'github'
    
    // Estados originales de la aplicación
    const [isLoading, setLoading] = useState(false);
    const [isInserting, setInserting] = useState(false);
    const [items, setItems] = useState([]);
    const [error, setError] = useState();

    function deleteItem(deleteId) {
      fetch(API_LIST+"/"+deleteId, {
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
        },
        (error) => {
          setError(error);
        }
      );
    }
    
    function toggleDone(event, id, description, done) {
      event.preventDefault();
      modifyItem(id, description, done).then(
        (result) => { reloadOneIteam(id); },
        (error) => { setError(error); }
      );
    }
    
    function reloadOneIteam(id){
      fetch(API_LIST+"/"+id)
        .then(response => {
          if (response.ok) {
            return response.json();
          } else {
            throw new Error('Something went wrong ...');
          }
        })
        .then(
          (result) => {
            const items2 = items.map(
              x => (x.id === id ? {
                 ...x,
                 'description':result.description,
                 'done': result.done
                } : x));
            setItems(items2);
          },
          (error) => {
            setError(error);
          });
    }
    
    function modifyItem(id, description, done) {
      var data = {"description": description, "done": done};
      return fetch(API_LIST+"/"+id, {
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
            setItems(result);
          },
          (error) => {
            setLoading(false);
            setError(error);
          });
    },
    [] // empty deps array [] means this useEffect will run once
    );
    
    function addItem(text){
      setInserting(true);
      var data = {};
      data.description = text;
      fetch(API_LIST, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(data),
      }).then((response) => {
        if (response.ok) {
          return response;
        } else {
          throw new Error('Something went wrong ...');
        }
      }).then(
        (result) => {
          var id = result.headers.get('location');
          var newItem = {"id": id, "description": text}
          setItems([newItem, ...items]);
          setInserting(false);
        },
        (error) => {
          setInserting(false);
          setError(error);
        }
      );
    }
    
    // Get counts of pending and completed items
    const pendingItems = items.filter(item => !item.done);
    const completedItems = items.filter(item => item.done);
    
    return (
      <div className="App">
        {/* Oracle Logo */}
        <img 
          src="https://imgs.search.brave.com/VP0I6z3w18_vEzuRoDlY0arRjFf9OdUsX3928ysRXmE/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9sb2dv/ZG93bmxvYWQub3Jn/L3dwLWNvbnRlbnQv/dXBsb2Fkcy8yMDE0/LzA0L29yYWNsZS1s/b2dvLTAucG5n" 
          alt="Oracle Logo" 
          className="oracle-logo" 
        />
        
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
        
        {/* Contenido de la pestaña de tareas */}
        {activeTab === 'tasks' && (
          <>
            <NewItem addItem={addItem} isInserting={isInserting}/>
            
            { error &&
              <div className="error-message">
                <p>Error: {error.message}</p>
              </div>
            }
            
            { isLoading ? (
              <div className="loading-container">
                <div className="loading-spinner"></div>
              </div>
            ) : (
              <div id="maincontent">
                {/* Pending Tasks */}
                <div className="task-section-header">
                  <h2>Tasks To Do <span className="task-count">{pendingItems.length}</span></h2>
                </div>
                
                <table id="itemlistNotDone" className="itemlist">
                  <TableBody>
                  {pendingItems.length === 0 ? (
                    <tr>
                      <td colSpan="3" className="empty-message">
                        No pending tasks. Add a task to get started!
                      </td>
                    </tr>
                  ) : (
                    pendingItems.map(item => (
                      <tr key={item.id}>
                        <td className="description">{item.description}</td>
                        <td className="date"><Moment format="MMM Do HH:mm">{item.createdAt}</Moment></td>
                        <td><Button variant="contained" className="DoneButton" onClick={(event) => toggleDone(event, item.id, item.description, !item.done)} size="small">
                              Done
                            </Button></td>
                      </tr>
                    ))
                  )}
                  </TableBody>
                </table>
                
                {/* Completed Tasks */}
                <h2 id="donelist">
                  Done items <span className="task-count">{completedItems.length}</span>
                </h2>
                
                <table id="itemlistDone" className="itemlist">
                  <TableBody>
                  {completedItems.length === 0 ? (
                    <tr>
                      <td colSpan="4" className="empty-message">
                        No completed tasks yet.
                      </td>
                    </tr>
                  ) : (
                    completedItems.map(item => (
                      <tr key={item.id}>
                        <td className="description">{item.description}</td>
                        <td className="date"><Moment format="MMM Do HH:mm">{item.createdAt}</Moment></td>
                        <td><Button variant="contained" className="DoneButton" onClick={(event) => toggleDone(event, item.id, item.description, !item.done)} size="small">
                              Undo
                            </Button></td>
                        <td><Button variant="contained" className="DeleteButton" onClick={() => deleteItem(item.id)} size="small">
                              Delete
                            </Button></td>
                      </tr>
                    ))
                  )}
                  </TableBody>
                </table>
              </div>
            )}
          </>
        )}
        
        {/* Contenido de la pestaña de integración GitHub */}
        {activeTab === 'github' && (
          <GitHubIntegration todos={items} />
        )}
      </div>
    );
}

export default App;