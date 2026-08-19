import * as React from 'react';
import { useState } from 'react';
import axios from 'axios';
import { CssVarsProvider, extendTheme, useColorScheme } from '@mui/joy/styles';
import GlobalStyles from '@mui/joy/GlobalStyles';
import CssBaseline from '@mui/joy/CssBaseline';
import Box from '@mui/joy/Box';
import Button from '@mui/joy/Button';
import FormControl from '@mui/joy/FormControl';
import FormLabel from '@mui/joy/FormLabel';
import IconButton from '@mui/joy/IconButton';
import Input from '@mui/joy/Input';
import Typography from '@mui/joy/Typography';
import Stack from '@mui/joy/Stack';

import DarkModeRoundedIcon from '@mui/icons-material/DarkModeRounded';
import LightModeRoundedIcon from '@mui/icons-material/LightModeRounded';
import BadgeRoundedIcon from '@mui/icons-material/BadgeRounded';






function ColorSchemeToggle(props) {
    const { onClick, ...other } = props;
    const { mode, setMode } = useColorScheme();
    const [mounted, setMounted] = React.useState(false);

    React.useEffect(() => {
        setMounted(true);
    }, []);

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
            {mode === 'light' ? (
                <DarkModeRoundedIcon />
            ) : (
                <LightModeRoundedIcon />
            )}
        </IconButton>
    );
}



const customTheme = extendTheme({
    defaultColorScheme: 'dark',
});

export default function Login({ setPage }) {
    const [data, setData] = useState({
        phoneNumber: '',
        otp: '',
    });
    const [otpSent, setOtpSent] = useState(false);
    const [otpTimer, setOtpTimer] = useState(0);
    const [loading, setLoading] = useState(false);
    const [sendingOtp, setSendingOtp] = useState(false);
    React.useEffect(() => {
        if (otpTimer <= 0) return;

        const timer = setInterval(() => {
            setOtpTimer((prev) => prev - 1);
        }, 1000);

        return () => clearInterval(timer);
    }, [otpTimer]);

    const handleChange = (e) => {
        const { name, value } = e.target;

        setData((prev) => ({
            ...prev,
            [name]: value,
        }));
    };

    const sendOtp = async () => {
        if (!data.phoneNumber.trim()) {
            alert('Please enter a phone number first');
            return;
        }

        try {
            setSendingOtp(true);

            await axios.post(
                'http://localhost:8080/api/v1/auth/send-otp',
                {
                    phone: data.phoneNumber,
                }
            );
            setOtpSent(true);
            setOtpTimer(30);
        } catch (err) {
            console.error(err);
        } finally {
            setSendingOtp(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        try {
            const { data: res } = await axios.post(
                'http://localhost:8080/api/v1/auth/verify-otp',
                {
                    phone: data.phoneNumber,
                    otp: data.otp,
                },
                {
                    withCredentials: true,
                }
            );

            // ✅ Backend tells us where to go
            if (res.newUser) {
                setPage('complete-profile');
            } else {
                setPage('home');
            }
        } catch (err) {
            console.error(err);
            alert('Invalid OTP');
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
                    transitionDelay:
                        'calc(var(--Transition-duration) + 0.1s)',
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
                    <Box
                        component="header"
                        sx={{
                            py: 3,
                            display: 'flex',
                            justifyContent: 'space-between',
                        }}
                    >
                        <Box
                            sx={{
                                gap: 2,
                                display: 'flex',
                                alignItems: 'center',
                            }}
                        >
                            <IconButton
                                variant="soft"
                                color="primary"
                                size="sm"
                            >
                                <BadgeRoundedIcon />
                            </IconButton>

                            <Typography level="title-lg">
                                Echo
                            </Typography>
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
                        <Stack sx={{ gap: 4, mb: 2 }}>
                            <Stack sx={{ gap: 1 }}>
                                <Typography component="h1" level="h3">
                                    Login with OTP
                                </Typography>


                            </Stack>
                        </Stack>

                        <form onSubmit={handleSubmit}>
                            <FormControl required>
                                <FormLabel>Phone Number</FormLabel>

                                <Input
                                    type="tel"
                                    name="phoneNumber"
                                    placeholder="Enter phone number"
                                    value={data.phoneNumber}
                                    onChange={handleChange}
                                    endDecorator={
                                        <Button
                                            size="sm"
                                            variant="soft"
                                            loading={sendingOtp}
                                            disabled={otpSent && otpTimer > 0}
                                            onClick={sendOtp}
                                        >
                                            {!otpSent
                                                ? 'Send OTP'
                                                : otpTimer > 0
                                                    ? `Resend OTP in ${otpTimer}s`
                                                    : 'Resend OTP'}
                                        </Button>
                                    }
                                />
                            </FormControl>

                            <FormControl required>
                                <FormLabel>OTP</FormLabel>

                                <Input
                                    type="text"
                                    name="otp"
                                    placeholder="Enter OTP"
                                    value={data.otp}
                                    onChange={handleChange}
                                />
                            </FormControl>

                            <Stack sx={{ gap: 4, mt: 2 }}>
                                <Button
                                    type="submit"
                                    fullWidth
                                    loading={loading}
                                >
                                    Verify OTP
                                </Button>
                            </Stack>
                        </form>
                    </Box>

                    <Box component="footer" sx={{ py: 3 }}>
                        <Typography
                            level="body-xs"
                            sx={{ textAlign: 'center' }}
                        >
                            © Echo {new Date().getFullYear()}
                        </Typography>
                    </Box>
                </Box>
            </Box>

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
                        'url(https://images.unsplash.com/photo-1527181152855-fc03fc7949c8?auto=format&w=1000&dpr=2)',
                    [theme.getColorSchemeSelector('dark')]: {
                        backgroundImage:
                            'url(https://images.unsplash.com/photo-1572072393749-3ca9c8ea0831?auto=format&w=1000&dpr=2)',
                    },
                })}
            />
        </CssVarsProvider>
    );
}

