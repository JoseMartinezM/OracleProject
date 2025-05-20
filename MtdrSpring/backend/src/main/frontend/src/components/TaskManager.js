import React, { useState, useEffect } from 'react';
import TaskTable from './TaskTable';
import { API_LIST, API_SPRINTS } from './API';

function TaskManager({ currentUser }) {
    const [tasks, setTasks] = useState([]);
    const [sprints, setSprints] = useState([]);
    const [selectedSprint, setSelectedSprint] = useState(null);
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        loadTasks();
        loadSprints();
    }, []);

    const loadTasks = async () => {
        setIsLoading(true);
        try {
        const response = await fetch(API_LIST);
        const data = await response.json();
        setTasks(data);
        } catch (error) {
        console.error('Error loading tasks:', error);
        } finally {
        setIsLoading(false);
        }
    };

    const loadSprints = async () => {
        try {
        const response = await fetch(API_SPRINTS);
        const data = await response.json();
        setSprints(data);
        } catch (error) {
        console.error('Error loading sprints:', error);
        }
    };

    return (
        <div>
        <h2>Tasks</h2>
        <TaskTable tasks={tasks} currentUser={currentUser} />
        </div>
    );
}

export default TaskManager;