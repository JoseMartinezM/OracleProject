import React, { useState } from 'react';
import { API_USERS } from './API';

function Login({ onLogin, loginError }) {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!username || !password) return;

        setIsLoading(true);

        try {
        const response = await fetch(`${API_USERS}/username/${username}`);
        if (!response.ok) throw new Error('Invalid credentials');

        const userData = await response.json();
        if (userData.password === password) {
            localStorage.setItem(
            'currentUser',
            JSON.stringify({
                id: userData.id,
                username: userData.username,
                name: userData.name,
                role: userData.role,
            })
            );
            onLogin(userData);
        } else {
            throw new Error('Invalid credentials');
        }
        } catch (error) {
        onLogin(null, error.message);
        } finally {
        setIsLoading(false);
        }
    };

    return (
        <div className="login-overlay">
        <div className="login-container">
            <h2>TODO App Login</h2>
            <form onSubmit={handleSubmit}>
            <input
                type="text"
                placeholder="Username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
            />
            <input
                type="password"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
            />
            <button type="submit" disabled={isLoading}>
                {isLoading ? 'Logging in...' : 'Login'}
            </button>
            {loginError && <div className="login-error">{loginError}</div>}
            </form>
        </div>
        </div>
    );
}

export default Login;