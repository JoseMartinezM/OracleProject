import React from 'react';

function TaskTable({ tasks, currentUser }) {
  return (
    <table>
      <thead>
        <tr>
          <th>Description</th>
          <th>Status</th>
          <th>Priority</th>
        </tr>
      </thead>
      <tbody>
        {tasks.map((task) => (
          <tr key={task.id}>
            <td>{task.description}</td>
            <td>{task.status}</td>
            <td>{task.priority}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default TaskTable;