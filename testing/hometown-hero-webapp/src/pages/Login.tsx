import {Button, Option, PasswordInput, Select, TextInput} from '@r3/r3-tooling-design-system/exports';

import FormContentWrapper from '@/components/FormContentWrapper/FormContentWrapper';
import LoginViz from '@/components/Visualizations/LoginViz';
import PageContentWrapper from '@/components/PageContentWrapper/PageContentWrapper';
import PageHeader from '@/components/PageHeader/PageHeader';
import { VNODE_HOME } from '@/constants/routes';
import VisualizationWrapper from '@/components/Visualizations/VisualizationWrapper';
import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import useUserContext from '@/contexts/userContext';

const Login = () => {
    const { username: savedUsername, password: savedPassword, login, saveLoginDetails, cluster: savedCluster } = useUserContext();

    const navigate = useNavigate();

    const [username, setUsername] = useState<string>(savedUsername);
    const [password, setPassword] = useState<string>(savedPassword);
    const [cluster, setCluster] = useState<string>(savedCluster || "cluster0");

    const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = event.target;
        if (name === 'username') {
            setUsername(value);
        }
        if (name === 'password') {
            setPassword(value);
        }
    };

    const handleSubmit = async () => {
        const loggedInSuccessfully = await login(username, password, cluster);
        if (!loggedInSuccessfully) {
            setUsername('');
            setPassword('');
            return;
        }
        saveLoginDetails(username, password, cluster);
        navigate(VNODE_HOME);
    };

    return (
        <PageContentWrapper>
            <PageHeader withBackButton>Login to V-Node</PageHeader>
            <FormContentWrapper>
                <TextInput
                    required
                    name="username"
                    label={'Username'}
                    value={username}
                    onChange={handleInputChange}
                    invalid={username.length === 0}
                />
                <PasswordInput
                    required
                    name="password"
                    label={'Password*'}
                    value={password}
                    onChange={handleInputChange}
                    invalid={password.length === 0}
                />
                <Select value={cluster} label="Cluster" onChange={(event) => setCluster(event.target.value)}>
                    <Option value="cluster0">Cluster 0</Option>
                    <Option value="cluster1">Cluster 1</Option>
                    <Option value="cluster2">Cluster 2</Option>
                </Select>
                <Button
                    className="h-12 w-32"
                    size={'large'}
                    variant={'primary'}
                    disabled={username.length === 0 || password.length === 0}
                    onClick={handleSubmit}
                >
                    Sign In
                </Button>
            </FormContentWrapper>
            <VisualizationWrapper width={700}>
                <LoginViz />
            </VisualizationWrapper>
        </PageContentWrapper>
    );
};

export default Login;
