import React from 'react'
import ReactDOM from 'react-dom/client'
import { CssVarsProvider } from '@mui/joy/styles'
import CssBaseline from '@mui/joy/CssBaseline'
import App from './App.jsx'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
    <CssVarsProvider defaultMode="dark">
        <CssBaseline />
        <App />
    </CssVarsProvider>
)