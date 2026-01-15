# Frontend Architecture

## Overview

The frontend is built with React.js and provides a modern, responsive user interface for students and instructors to interact with the course platform. The architecture follows React best practices with component-based design, state management, and efficient data fetching.

## Technology Stack

### Core Framework
- **React.js 18+** - Component-based UI library
- **JavaScript (ES6+)** - Modern JavaScript with ES6+ features
- **Vite** - Fast build tool and development server

### State Management
- **Redux Toolkit** - Predictable state management
- **RTK Query** - Data fetching and caching
- **React Context** - Local component state

### UI Components & Styling
- **Material-UI (MUI)** - Component library
- **Emotion** - CSS-in-JS styling
- **React Router** - Client-side routing

### HTTP Client & Utilities
- **Axios** - HTTP client with interceptors
- **React Hook Form** - Form management
- **Yup** - Form validation
- **Date-fns** - Date manipulation

### Development Tools
- **ESLint** - Code linting
- **Prettier** - Code formatting
- **Husky** - Git hooks
- **Jest + React Testing Library** - Testing

## Application Structure

```
src/
├── components/           # Reusable UI components
│   ├── common/          # Generic components (Button, Input, etc.)
│   ├── layout/          # Layout components (Header, Sidebar, etc.)
│   ├── forms/           # Form components
│   └── ui/              # UI-specific components
├── pages/               # Page components
│   ├── auth/            # Authentication pages
│   ├── dashboard/       # Dashboard pages
│   ├── courses/         # Course-related pages
│   └── profile/         # User profile pages
├── hooks/               # Custom React hooks
├── services/            # API service layer
├── store/               # Redux store configuration
│   ├── slices/          # Redux slices
│   └── api/             # RTK Query API slices
├── utils/               # Utility functions
├── types/               # JavaScript type definitions (JSDoc or PropTypes)
├── constants/           # Application constants
├── theme/               # Theme configuration
└── assets/              # Static assets
```

## Component Architecture

### Component Types

#### 1. Page Components
- Route-level components
- Handle page-specific logic
- Compose smaller components
- Manage page-level state

```jsx
// pages/courses/CourseListPage.jsx
const CourseListPage = () => {
  const [filters, setFilters] = useState({});
  const { data: courses, isLoading } = useGetCoursesQuery(filters);

  return (
    <PageLayout>
      <CourseFilters onChange={setFilters} />
      <CourseGrid courses={courses} loading={isLoading} />
    </PageLayout>
  );
};
```

#### 2. Feature Components
- Business logic components
- Handle feature-specific state
- Reusable within pages

```tsx
// components/courses/CourseCard.tsx
interface CourseCardProps {
  course: Course;
  onEnroll?: (courseId: string) => void;
}

const CourseCard: React.FC<CourseCardProps> = ({ course, onEnroll }) => {
  const { user } = useAuth();
  const [enroll, { isLoading }] = useEnrollInCourseMutation();

  const handleEnroll = async () => {
    if (onEnroll) {
      onEnroll(course.id);
    } else {
      await enroll(course.id);
    }
  };

  return (
    <Card>
      <CardMedia image={course.thumbnailUrl} />
      <CardContent>
        <Typography variant="h6">{course.title}</Typography>
        <Typography variant="body2">{course.description}</Typography>
        <Button
          onClick={handleEnroll}
          disabled={isLoading || !user}
        >
          {isLoading ? 'Enrolling...' : 'Enroll Now'}
        </Button>
      </CardContent>
    </Card>
  );
};
```

#### 3. UI Components
- Pure presentational components
- No business logic
- Highly reusable

```tsx
// components/ui/Button.tsx
interface ButtonProps extends MuiButtonProps {
  variant?: 'primary' | 'secondary' | 'danger';
  size?: 'small' | 'medium' | 'large';
}

const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'medium',
  children,
  ...props
}) => {
  return (
    <MuiButton
      variant={variant === 'primary' ? 'contained' : 'outlined'}
      size={size}
      color={variant === 'danger' ? 'error' : 'primary'}
      {...props}
    >
      {children}
    </MuiButton>
  );
};
```

## State Management

### Redux Store Structure

```typescript
// store/index.ts
import { configureStore } from '@reduxjs/toolkit';
import { api } from './api';
import authReducer from './slices/authSlice';
import uiReducer from './slices/uiSlice';

export const store = configureStore({
  reducer: {
    [api.reducerPath]: api.reducer,
    auth: authReducer,
    ui: uiReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(api.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
```

### RTK Query API Layer

```typescript
// store/api/coursesApi.ts
import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

export const coursesApi = createApi({
  reducerPath: 'coursesApi',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api',
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.token;
      if (token) {
        headers.set('authorization', `Bearer ${token}`);
      }
      return headers;
    },
  }),
  tagTypes: ['Course', 'Enrollment'],
  endpoints: (builder) => ({
    getCourses: builder.query<Course[], CourseFilters>({
      query: (filters) => ({
        url: '/courses',
        params: filters,
      }),
      providesTags: ['Course'],
    }),
    getCourse: builder.query<Course, string>({
      query: (id) => `/courses/${id}`,
      providesTags: (result, error, id) => [{ type: 'Course', id }],
    }),
    createCourse: builder.mutation<Course, CourseCreateData>({
      query: (course) => ({
        url: '/courses',
        method: 'POST',
        body: course,
      }),
      invalidatesTags: ['Course'],
    }),
    enrollInCourse: builder.mutation<Enrollment, string>({
      query: (courseId) => ({
        url: '/enrollments',
        method: 'POST',
        body: { courseId },
      }),
      invalidatesTags: ['Enrollment'],
    }),
  }),
});

export const {
  useGetCoursesQuery,
  useGetCourseQuery,
  useCreateCourseMutation,
  useEnrollInCourseMutation,
} = coursesApi;
```

### Auth Slice

```typescript
// store/slices/authSlice.ts
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface AuthState {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  loading: boolean;
}

const initialState: AuthState = {
  user: null,
  token: null,
  refreshToken: null,
  isAuthenticated: false,
  loading: false,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<AuthResponse>) => {
      const { user, accessToken, refreshToken } = action.payload;
      state.user = user;
      state.token = accessToken;
      state.refreshToken = refreshToken;
      state.isAuthenticated = true;
    },
    logout: (state) => {
      state.user = null;
      state.token = null;
      state.refreshToken = null;
      state.isAuthenticated = false;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
  },
});

export const { setCredentials, logout, setLoading } = authSlice.actions;
export default authSlice.reducer;
```

## Routing Architecture

### Route Configuration

```typescript
// App.tsx
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { PrivateRoute, PublicRoute } from './components/auth/RouteGuards';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
        <Route path="/register" element={<PublicRoute><RegisterPage /></PublicRoute>} />
        <Route path="/courses" element={<CourseListPage />} />

        {/* Protected Routes */}
        <Route path="/dashboard" element={<PrivateRoute><DashboardPage /></PrivateRoute>} />
        <Route path="/courses/:id" element={<PrivateRoute><CourseDetailPage /></PrivateRoute>} />
        <Route path="/profile" element={<PrivateRoute><ProfilePage /></PrivateRoute>} />

        {/* Instructor Routes */}
        <Route path="/instructor/courses" element={
          <PrivateRoute roles={['INSTRUCTOR', 'ADMIN']}>
            <InstructorCoursesPage />
          </PrivateRoute>
        } />

        {/* Admin Routes */}
        <Route path="/admin/*" element={
          <PrivateRoute roles={['ADMIN']}>
            <AdminLayout />
          </PrivateRoute>
        } />
      </Routes>
    </BrowserRouter>
  );
}
```

### Route Guards

```typescript
// components/auth/RouteGuards.tsx
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

interface PrivateRouteProps {
  children: React.ReactNode;
  roles?: string[];
}

export const PrivateRoute: React.FC<PrivateRouteProps> = ({ children, roles }) => {
  const { isAuthenticated, user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <LoadingSpinner />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (roles && user && !roles.includes(user.role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <>{children}</>;
};

export const PublicRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return <LoadingSpinner />;
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
};
```

## Custom Hooks

### Authentication Hook

```typescript
// hooks/useAuth.ts
import { useSelector, useDispatch } from 'react-redux';
import { RootState, AppDispatch } from '../store';
import { logout, setLoading } from '../store/slices/authSlice';
import { useLoginMutation, useRefreshTokenMutation } from '../store/api/authApi';

export const useAuth = () => {
  const dispatch = useDispatch<AppDispatch>();
  const { user, token, isAuthenticated, loading } = useSelector(
    (state: RootState) => state.auth
  );

  const [login, { isLoading: loginLoading }] = useLoginMutation();
  const [refreshToken, { isLoading: refreshLoading }] = useRefreshTokenMutation();

  const handleLogin = async (credentials: LoginCredentials) => {
    try {
      dispatch(setLoading(true));
      const result = await login(credentials).unwrap();
      dispatch(setCredentials(result));
      return result;
    } catch (error) {
      throw error;
    } finally {
      dispatch(setLoading(false));
    }
  };

  const handleLogout = () => {
    dispatch(logout());
    // Clear any cached data
    localStorage.removeItem('persist:root');
  };

  const handleRefreshToken = async () => {
    try {
      const result = await refreshToken().unwrap();
      dispatch(setCredentials(result));
      return result;
    } catch (error) {
      handleLogout();
      throw error;
    }
  };

  return {
    user,
    token,
    isAuthenticated,
    loading: loading || loginLoading || refreshLoading,
    login: handleLogin,
    logout: handleLogout,
    refreshToken: handleRefreshToken,
  };
};
```

### Form Hook

```typescript
// hooks/useForm.ts
import { useState, useCallback } from 'react';
import { useForm as useReactHookForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { ObjectSchema } from 'yup';

export const useForm = <T extends Record<string, any>>(
  schema: ObjectSchema<T>,
  defaultValues?: Partial<T>
) => {
  const [isSubmitting, setIsSubmitting] = useState(false);

  const form = useReactHookForm<T>({
    resolver: yupResolver(schema),
    defaultValues,
  });

  const handleSubmit = useCallback(
    (onSubmit: (data: T) => Promise<void> | void) =>
      form.handleSubmit(async (data) => {
        setIsSubmitting(true);
        try {
          await onSubmit(data);
        } finally {
          setIsSubmitting(false);
        }
      }),
    [form]
  );

  return {
    ...form,
    isSubmitting,
    handleSubmit,
  };
};
```

## API Integration

### Axios Configuration

```typescript
// services/api.ts
import axios, { AxiosInstance, AxiosResponse } from 'axios';
import { store } from '../store';
import { logout } from '../store/slices/authSlice';

const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 10000,
});

// Request interceptor
api.interceptors.request.use(
  (config) => {
    const token = store.getState().auth.token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
api.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error) => {
    if (error.response?.status === 401) {
      store.dispatch(logout());
    }
    return Promise.reject(error);
  }
);

export default api;
```

### Error Handling

```typescript
// utils/errorHandler.ts
export const handleApiError = (error: any): string => {
  if (error.response?.data?.error?.message) {
    return error.response.data.error.message;
  }

  if (error.response?.status === 400) {
    return 'Invalid request. Please check your input.';
  }

  if (error.response?.status === 401) {
    return 'Authentication required. Please log in again.';
  }

  if (error.response?.status === 403) {
    return 'You do not have permission to perform this action.';
  }

  if (error.response?.status === 404) {
    return 'The requested resource was not found.';
  }

  if (error.response?.status >= 500) {
    return 'Server error. Please try again later.';
  }

  return 'An unexpected error occurred. Please try again.';
};
```

## Theme and Styling

### Theme Configuration

```typescript
// theme/index.ts
import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
      light: '#42a5f5',
      dark: '#1565c0',
    },
    secondary: {
      main: '#dc004e',
      light: '#ff5983',
      dark: '#9a0036',
    },
    background: {
      default: '#f5f5f5',
      paper: '#ffffff',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
    h1: {
      fontSize: '2.5rem',
      fontWeight: 500,
    },
    h2: {
      fontSize: '2rem',
      fontWeight: 500,
    },
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          textTransform: 'none',
        },
      },
    },
  },
});
```

## Performance Optimization

### Code Splitting

```typescript
// App.tsx
import { lazy, Suspense } from 'react';

const DashboardPage = lazy(() => import('./pages/dashboard/DashboardPage'));
const CourseDetailPage = lazy(() => import('./pages/courses/CourseDetailPage'));

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<LoadingSpinner />}>
        <Routes>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/courses/:id" element={<CourseDetailPage />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
```

### Memoization

```typescript
// components/courses/CourseList.tsx
import { memo } from 'react';

interface CourseListProps {
  courses: Course[];
  onCourseClick: (course: Course) => void;
}

const CourseList: React.FC<CourseListProps> = memo(({ courses, onCourseClick }) => {
  return (
    <div>
      {courses.map((course) => (
        <CourseCard
          key={course.id}
          course={course}
          onClick={() => onCourseClick(course)}
        />
      ))}
    </div>
  );
});
```

## Testing Strategy

### Component Testing

```typescript
// components/courses/CourseCard.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { CourseCard } from './CourseCard';

const mockCourse: Course = {
  id: '1',
  title: 'Test Course',
  description: 'Test Description',
  instructor: 'Test Instructor',
  price: 99.99,
};

describe('CourseCard', () => {
  it('renders course information', () => {
    render(<CourseCard course={mockCourse} />);

    expect(screen.getByText('Test Course')).toBeInTheDocument();
    expect(screen.getByText('Test Description')).toBeInTheDocument();
    expect(screen.getByText('Test Instructor')).toBeInTheDocument();
  });

  it('calls onClick when clicked', () => {
    const mockOnClick = jest.fn();
    render(<CourseCard course={mockCourse} onClick={mockOnClick} />);

    fireEvent.click(screen.getByRole('button', { name: /enroll/i }));

    expect(mockOnClick).toHaveBeenCalledWith(mockCourse);
  });
});
```

### Integration Testing

```typescript
// pages/courses/CourseListPage.test.tsx
import { render, screen, waitFor } from '@testing-library/react';
import { CourseListPage } from './CourseListPage';
import { rest } from 'msw';
import { setupServer } from 'msw/node';

const server = setupServer(
  rest.get('/api/courses', (req, res, ctx) => {
    return res(ctx.json([mockCourse]));
  })
);

describe('CourseListPage', () => {
  beforeAll(() => server.listen());
  afterEach(() => server.resetHandlers());
  afterAll(() => server.close());

  it('loads and displays courses', async () => {
    render(<CourseListPage />);

    await waitFor(() => {
      expect(screen.getByText('Test Course')).toBeInTheDocument();
    });
  });
});
```

## Build and Deployment

### Build Configuration

```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'dist',
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom'],
          ui: ['@mui/material', '@emotion/react'],
          utils: ['axios', 'date-fns'],
        },
      },
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

### Environment Configuration

```typescript
// config/env.ts
export const config = {
  apiUrl: import.meta.env.VITE_API_URL || '/api',
  environment: import.meta.env.MODE,
  version: import.meta.env.VITE_APP_VERSION || '1.0.0',
  sentry: {
    dsn: import.meta.env.VITE_SENTRY_DSN,
    environment: import.meta.env.MODE,
  },
};
```

This frontend architecture provides a scalable, maintainable, and performant foundation for the course learning platform, with clear separation of concerns and modern React patterns.
