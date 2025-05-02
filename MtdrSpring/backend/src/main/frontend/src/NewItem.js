import React, { useState } from "react";
import Button from '@mui/material/Button';
import { TextField, Select, MenuItem, FormControl, InputLabel, IconButton } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';

function NewItem(props) {
  const [item, setItem] = useState('');
  const [steps, setSteps] = useState('');
  const [priority, setPriority] = useState('Medium');
  const [sprintId, setSprintId] = useState('');
  const [assignedTo, setAssignedTo] = useState('');
  const [expanded, setExpanded] = useState(false);
  
  function handleSubmit(e) {
    e.preventDefault();
    if (!item.trim()) {
      return;
    }
    // Pasamos descripción, pasos, prioridad, sprint y desarrollador asignado
    props.addItem(item, steps, priority, sprintId || null, assignedTo || null);
    // Reseteamos los campos
    setItem("");
    setSteps("");
    setPriority("Medium");
    setSprintId("");
    setAssignedTo("");
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
            
            {/* Selector de Desarrollador */}
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
                      {dev.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </div>
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