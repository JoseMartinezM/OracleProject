import React, { useState } from "react";
import Button from '@mui/material/Button';
import { TextField, Select, MenuItem, FormControl, InputLabel, IconButton } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';

function NewItem(props) {
  const [item, setItem] = useState('');
  const [steps, setSteps] = useState('');
  const [priority, setPriority] = useState('Medium');
  const [expanded, setExpanded] = useState(false);
  
  function handleSubmit(e) {
    e.preventDefault();
    if (!item.trim()) {
      return;
    }
    // Pasamos solo descripción, pasos, y prioridad (no el estado)
    props.addItem(item, steps, priority);
    // Reseteamos los campos
    setItem("");
    setSteps("");
    setPriority("Medium");
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
          </div>
        )}
        
        <Button
          className="AddButton"
          variant="contained"
          disabled={props.isInserting}
          onClick={!props.isInserting ? handleSubmit : null}
          type="submit"
          size="small"
        >
          {props.isInserting ? 'Adding…' : 'Add'}
        </Button>
      </form>
    </div>
  );
}

export default NewItem;