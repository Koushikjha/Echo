import * as React from 'react';
import { useState } from 'react';
import { CssVarsProvider, extendTheme, useColorScheme } from '@mui/joy/styles';
import GlobalStyles from '@mui/joy/GlobalStyles';
import CssBaseline from '@mui/joy/CssBaseline';
import Box from '@mui/joy/Box';
import Button from '@mui/joy/Button';
import FormControl from '@mui/joy/FormControl';
import FormLabel from '@mui/joy/FormLabel';
import FormHelperText from '@mui/joy/FormHelperText';
import IconButton from '@mui/joy/IconButton';
import Link from '@mui/joy/Link';
import Input from '@mui/joy/Input';
import Typography from '@mui/joy/Typography';
import Stack from '@mui/joy/Stack';
import InfoOutlined from '@mui/icons-material/InfoOutlined';
import DarkModeRoundedIcon from '@mui/icons-material/DarkModeRounded';
import LightModeRoundedIcon from '@mui/icons-material/LightModeRounded';
import BadgeRoundedIcon from '@mui/icons-material/BadgeRounded';
import api from './api';

function ColorSchemeToggle(props) {
    const { onClick, ...other } = props;
    const { mode, setMode } = useColorScheme();
    const [mounted, setMounted] = React.useState(false);
    React.useEffect(() => setMounted(true), []);

    return (
        <IconButton
            aria-label="toggle light/dark mode"
            size="sm"
            variant="outlined"
            disabled={!mounted}
            onClick={(event) => {
                setMode(mode === 'light' ? 'dark' : 'light');
                onClick?.(event);
            }}
            {...other}
        >
            {mode === 'light' ? <DarkModeRoundedIcon /> : <LightModeRoundedIcon />}
        </IconButton>
    );
}

const customTheme = extendTheme({ defaultColorScheme: 'dark' });

export default function Register({ setPage }) {
    const [data, setData] = useState({ username: '', password: '', confirmPassword: '' });
    const [loading, setLoading] = useState(false);

    // Check if passwords match
    const passwordsMatch = data.password === data.confirmPassword;
    const showPasswordError = data.confirmPassword.length > 0 && !passwordsMatch;

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!passwordsMatch) return;

        setLoading(true);
        try {
            await api.post('/auth/register', {
                username: data.username,
                password: data.password
            });
            setPage('login');
        } catch (err) {
            console.error(err);
            alert('Registration failed 💀');
        } finally {
            setLoading(false);
        }
    };

    return (
        <CssVarsProvider theme={customTheme} disableTransitionOnChange>
            <CssBaseline />
            <GlobalStyles
                styles={{
                    ':root': {
                        '--Form-maxWidth': '800px',
                        '--Transition-duration': '0.4s',
                    },
                }}
            />
            <Box
                sx={(theme) => ({
                    width: { xs: '100%', md: '50vw' },
                    transition: 'width var(--Transition-duration)',
                    transitionDelay: 'calc(var(--Transition-duration) + 0.1s)',
                    position: 'relative',
                    zIndex: 1,
                    display: 'flex',
                    justifyContent: 'flex-end',
                    backdropFilter: 'blur(12px)',
                    backgroundColor: 'rgba(255 255 255 / 0.2)',
                    [theme.getColorSchemeSelector('dark')]: {
                        backgroundColor: 'rgba(19 19 24 / 0.4)',
                    },
                })}
            >
                <Box
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        minHeight: '100dvh',
                        width: '100%',
                        px: 2,
                    }}
                >
                    <Box component="header" sx={{ py: 3, display: 'flex', justifyContent: 'space-between' }}>
                        <Box sx={{ gap: 2, display: 'flex', alignItems: 'center' }}>
                            <IconButton variant="soft" color="primary" size="sm">
                                <BadgeRoundedIcon />
                            </IconButton>
                            <Typography level="title-lg">Vichar-Vimarsh</Typography>
                        </Box>
                        <ColorSchemeToggle />
                    </Box>

                    <Box
                        component="main"
                        sx={{
                            my: 'auto',
                            py: 2,
                            pb: 5,
                            display: 'flex',
                            flexDirection: 'column',
                            gap: 2,
                            width: 400,
                            maxWidth: '100%',
                            mx: 'auto',
                            borderRadius: 'sm',
                            '& form': {
                                display: 'flex',
                                flexDirection: 'column',
                                gap: 2,
                            },
                        }}
                    >
                        <Stack sx={{ gap: 1, mb: 2 }}>
                            <Typography component="h1" level="h3">
                                Create an account
                            </Typography>
                            <Typography level="body-sm">
                                Already have an account?{' '}
                                <Link
                                    component="button"
                                    onClick={() => setPage('login')}
                                    level="title-sm"
                                >
                                    Sign in!
                                </Link>
                            </Typography>
                        </Stack>

                        <form onSubmit={handleSubmit}>
                            <FormControl required>
                                <FormLabel>Username</FormLabel>
                                <Input
                                    name="username"
                                    type="text"
                                    placeholder="Choose a username"
                                    value={data.username}
                                    onChange={(e) => setData({ ...data, username: e.target.value })}
                                />
                            </FormControl>

                            <FormControl required error={showPasswordError}>
                                <FormLabel>Password</FormLabel>
                                <Input
                                    name="password"
                                    type="password"
                                    placeholder="••••••••"
                                    value={data.password}
                                    onChange={(e) => setData({ ...data, password: e.target.value })}
                                />
                            </FormControl>

                            <FormControl required error={showPasswordError}>
                                <FormLabel>Confirm Password</FormLabel>
                                <Input
                                    name="confirmPassword"
                                    type="password"
                                    placeholder="••••••••"
                                    value={data.confirmPassword}
                                    onChange={(e) => setData({ ...data, confirmPassword: e.target.value })}
                                />
                                {showPasswordError && (
                                    <FormHelperText>
                                        <InfoOutlined />
                                        Passwords do not match.
                                    </FormHelperText>
                                )}
                            </FormControl>

                            <Stack sx={{ gap: 4, mt: 2 }}>
                                <Button
                                    type="submit"
                                    fullWidth
                                    loading={loading}
                                    disabled={showPasswordError}
                                >
                                    Register
                                </Button>
                            </Stack>
                        </form>
                    </Box>

                    <Box component="footer" sx={{ py: 3 }}>
                        <Typography level="body-xs" sx={{ textAlign: 'center' }}>
                            © V&V {new Date().getFullYear()}
                        </Typography>
                    </Box>
                </Box>
            </Box>

            {/* Background Section */}
            <Box
                sx={(theme) => ({
                    height: '100%',
                    position: 'fixed',
                    right: 0,
                    top: 0,
                    bottom: 0,
                    left: { xs: 0, md: '50vw' },
                    backgroundColor: 'background.level1',
                    backgroundSize: 'cover',
                    backgroundPosition: 'center',
                    backgroundRepeat: 'no-repeat',
                    backgroundImage:
                        'url(https://images.unsplash.com/photo-1572072393749-3ca9c8ea0831?auto=format&w=1000&dpr=2)',
                })}
            />
        </CssVarsProvider>
    );
}