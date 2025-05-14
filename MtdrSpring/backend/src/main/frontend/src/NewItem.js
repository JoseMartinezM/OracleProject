import React, { useState } from "react";
import Button from '@mui/material/Button';
import { TextField, Select, MenuItem, FormControl, InputLabel, IconButton } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import AccessTimeIcon from '@mui/icons-material/AccessTime';

function NewItem(props) {
  const [item, setItem] = useState('');
  const [steps, setSteps] = useState('');
  const [priority, setPriority] = useState('Medium');
  const [sprintId, setSprintId] = useState('');
  const [assignedTo, setAssignedTo] = useState('');
  const [estimatedHours, setEstimatedHours] = useState('');
  const [expanded, setExpanded] = useState(false);
  
  // Verificar si el usuario actual es un desarrollador
  const isDeveloper = props.currentUser?.role === 'Developer';
  
  function handleSubmit(e) {
    e.preventDefault();
    if (!item.trim()) {
      return;
    }
    
    // Si el usuario es developer, auto-asignar la tarea
    const actualAssignedTo = isDeveloper ? props.currentUser.id : (assignedTo || null);
    
    // Convertir horas estimadas a número si existe
    const parsedEstimatedHours = estimatedHours ? parseFloat(estimatedHours) : null;
    
    // Pasamos descripción, pasos, prioridad, sprint, desarrollador asignado y horas estimadas
    props.addItem(
      item, 
      steps, 
      priority, 
      sprintId ? parseInt(sprintId) : null, 
      actualAssignedTo ? parseInt(actualAssignedTo) : null,
      parsedEstimatedHours
    );
    
    // Reseteamos los campos
    setItem("");
    setSteps("");
    setPriority("Medium");
    setSprintId("");
    setAssignedTo("");
    setEstimatedHours("");
    
    // Opcional: cerrar los campos expandidos después de enviar
    // setExpanded(false);
  }
  
  function handleChange(e) {
    setItem(e.target.value);
  }
  
  function toggleExpanded() {
    setExpanded(!expanded);
  }
  
  return (
    <div id="newinputform">
      <form onSubmit={handleSubmit}>
        <div className="form-main-row">
          <input
            id="newiteminput"
            placeholder="What needs to be done?"
            type="text"
            autoComplete="off"
            value={item}
            onChange={handleChange}
            disabled={props.isInserting}
            onKeyDown={event => {
              if (event.key === 'Enter' && !expanded) {
                handleSubmit(event);
              }
            }}
          />
          <IconButton 
            onClick={toggleExpanded} 
            className="expand-button"
            color="inherit"
          >
            {expanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
          </IconButton>
        </div>
        
        {expanded && (
          <div className="form-expanded">
            <div className="form-row">
              <TextField
                label="Steps"
                multiline
                rows={3}
                placeholder="Step 1: ...\nStep 2: ..."
                value={steps}
                onChange={(e) => setSteps(e.target.value)}
                variant="outlined"
                fullWidth
                InputProps={{
                  className: "custom-text-field"
                }}
                InputLabelProps={{
                  className: "custom-input-label"
                }}
                disabled={props.isInserting}
              />
            </div>
            
            <div className="form-row form-selects">
              {/* Campo de prioridad */}
              <FormControl variant="outlined" className="priority-select" fullWidth>
                <InputLabel id="priority-label" className="custom-input-label">Priority</InputLabel>
                <Select
                  labelId="priority-label"
                  value={priority}
                  onChange={(e) => setPriority(e.target.value)}
                  label="Priority"
                  className="custom-select"
                  disabled={props.isInserting}
                >
                  <MenuItem value="Low">Low</MenuItem>
                  <MenuItem value="Medium">Medium</MenuItem>
                  <MenuItem value="High">High</MenuItem>
                  <MenuItem value="Critical">Critical</MenuItem>
                </Select>
              </FormControl>
              
              {/* Campo de horas estimadas - NUEVO */}
              <FormControl variant="outlined" className="hours-select" fullWidth>
                <TextField
                  label="Estimated Hours"
                  type="number"
                  value={estimatedHours}
                  onChange={(e) => setEstimatedHours(e.target.value)}
                  placeholder="e.g., 2.5"
                  InputProps={{
                    className: "custom-text-field",
                    startAdornment: (
                      <AccessTimeIcon fontSize="small" style={{ marginRight: '8px', opacity: 0.7 }} />
                    ),
                  }}
                  InputLabelProps={{
                    className: "custom-input-label"
                  }}
                  inputProps={{ 
                    min: "0.25", 
                    step: "0.25" 
                  }}
                  disabled={props.isInserting}
                  variant="outlined"
                  fullWidth
                />
              </FormControl>
            </div>
            
            {/* Selector de Sprint */}
            <div className="form-row form-selects">
              <FormControl variant="outlined" className="sprint-select" fullWidth>
                <InputLabel id="sprint-label" className="custom-input-label">Sprint</InputLabel>
                <Select
                  labelId="sprint-label"
                  value={sprintId}
                  onChange={(e) => setSprintId(e.target.value)}
                  label="Sprint"
                  className="custom-select"
                  disabled={props.isInserting}
                >
                  <MenuItem value="">None</MenuItem>
                  {props.sprints && props.sprints.map(sprint => (
                    <MenuItem key={sprint.id} value={sprint.id}>
                      {sprint.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </div>
            
            {/* Selector de Desarrollador - Solo visible para no-developers */}
            {!isDeveloper && (
              <div className="form-row form-selects">
                <FormControl variant="outlined" className="developer-select" fullWidth>
                  <InputLabel id="developer-label" className="custom-input-label">Assigned To</InputLabel>
                  <Select
                    labelId="developer-label"
                    value={assignedTo}
                    onChange={(e) => setAssignedTo(e.target.value)}
                    label="Assigned To"
                    className="custom-select"
                    disabled={props.isInserting}
                  >
                    <MenuItem value="">None</MenuItem>
                    {props.developers && props.developers.map(dev => (
                      <MenuItem key={dev.id} value={dev.id}>
                        {dev.name || dev.username}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </div>
            )}
            
            {/* Para desarrolladores, mostrar que la tarea se asignará a sí mismos */}
            {isDeveloper && (
              <div className="form-row">
                <div className="self-assign-notice">
                  <span>Task will be automatically assigned to you</span>
                </div>
              </div>
            )}
          </div>
        )}
        
        <div className="form-row">
          <Button 
            variant="contained" 
            type="submit" 
            className="submit-button" 
            disabled={props.isInserting || !item.trim()}
          >
            {props.isInserting ? 'Adding...' : 'Add Task'}
          </Button>
        </div>
      </form>
    </div>
  );
}

export default NewItem;