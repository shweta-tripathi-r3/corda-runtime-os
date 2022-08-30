import {
    Button,
    Checkbox,
    NotificationService,
    Option,
    PasswordInput, Select,
    TextInput
} from '@r3/r3-tooling-design-system/exports';
import { LOGIN, VNODE_HOME } from '@/constants/routes';
import { addPermissionToRole, addRoleToUser, createPermission, createRole, createUser, createVNode } from './utils';
import { useEffect, useState } from 'react';

import FormContentWrapper from '@/components/FormContentWrapper/FormContentWrapper';
import PageContentWrapper from '@/components/PageContentWrapper/PageContentWrapper';
import PageHeader from '@/components/PageHeader/PageHeader';
import RegisterViz from '@/components/Visualizations/RegisterViz';
import { VirtualNode } from '@/models/virtualnode';
import VisualizationWrapper from '@/components/Visualizations/VisualizationWrapper';
import apiCall from '@/api/apiCall';
import { trackPromise } from 'react-promise-tracker';
import useAppDataContext from '@/contexts/appDataContext';
import { useNavigate } from 'react-router-dom';
import useUserContext from '@/contexts/userContext';

const Register = () => {
    const { login, saveLoginDetails, username: savedUserUsername, password: savedUserPassword, cluster: savedUserCluster } = useUserContext();
    const navigate = useNavigate();

    const [username, setUsername] = useState<string>('');
    const [password, setPassword] = useState<string>('');
    const [cluster, setCluster] = useState<string>('cluster0');
    const [confirmPassword, setConfirmPassword] = useState<string>('');
    const [newVNode, setNewVNode] = useState<VirtualNode | undefined>(undefined);

    const { refreshVNodes, cpiList, refreshCpiList } = useAppDataContext();

    useEffect(() => {
        refreshCpiList();
    }, []);

    const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = event.target;
        if (name === 'username') {
            setUsername(value);
        }
        if (name === 'password') {
            setPassword(value);
        }
        if (name === 'confirm_password') {
            setConfirmPassword(value);
        }
    };

    const handleVNodeLogin = async () => {
        const isLoggedIn = await login(savedUserUsername, savedUserPassword, savedUserCluster);
        if (!isLoggedIn) return;
        navigate(VNODE_HOME);
    };

    const handleSubmit = async () => {
        setNewVNode(undefined);
        //If theres no cpis prevent user from registering
        if (cpiList.length === 0) {
            NotificationService.notify(
                `No CPIs are uploaded to the cluster. Cannot register a new VNode and User.`,
                'Error',
                'danger'
            );
            return;
        }

        const cpiFileChecksum = cpiList[0].cpiFileChecksum;

        const ipLocResponse = await apiCall({ method: 'get', path: 'https://ipapi.co/json/' });
        let city = 'City';
        if (!ipLocResponse.error) {
            city = ipLocResponse.data.country_capital;
        }

        const x500Name = `O=${username} node, L=${city}, C=${cluster}`;

        const vNodeCreated = await createVNode(x500Name, cpiFileChecksum, cluster);
        if (!vNodeCreated) return;

        //give some time for vNodes list to update
        await new Promise((r) => setTimeout(r, 1000));

        const updatedVNodes = await refreshVNodes();

        const newNode = updatedVNodes.find((vNode) => vNode.holdingIdentity.x500Name === x500Name);

        if (!newNode) {
            NotificationService.notify(
                `Could not find newly created VNode with x500 name: ${x500Name}.`,
                'Error',
                'danger'
            );
            return;
        }

        setNewVNode(newNode);

        const userCreated = await createUser(username, password, newNode.holdingIdentity.shortHash, cluster);
        if (!userCreated) return;

        const postPermissionId = await createPermission(
            `POST:/api/v1/flow/${newNode.holdingIdentity.shortHash}`,
            'ALLOW',
            cluster
        );
        if (!postPermissionId) return;

        const getPermissionId = await createPermission(
            `GET:/api/v1/flow/${newNode.holdingIdentity.shortHash}/.*`,
            'ALLOW',
            cluster
        );
        if (!getPermissionId) return;

        const userPermissionId = await createPermission(`GET:/api/v1/user\\?loginname=${username}`, 'ALLOW', cluster);
        if (!userPermissionId) return;

        const virtualNodesListPermissionId = await createPermission(`GET:/api/v1/virtualnode`, 'ALLOW', cluster);
        if (!virtualNodesListPermissionId) return;

        const roleId = await createRole(cluster);
        if (!roleId) return;

        const addedPostPermission = await addPermissionToRole(postPermissionId, roleId, cluster);
        if (!addedPostPermission) return;

        const addedGetPermission = await addPermissionToRole(getPermissionId, roleId, cluster);
        if (!addedGetPermission) return;

        const addedUserPermission = await addPermissionToRole(userPermissionId, roleId, cluster);
        if (!addedUserPermission) return;

        const addedVirtualNodesPermission = await addPermissionToRole(virtualNodesListPermissionId, roleId, cluster);
        if (!addedVirtualNodesPermission) return;

        NotificationService.notify(`Created permissions and added to new role for user!`, 'Success!', 'success');

        const addedRoleToUser = await addRoleToUser(username, roleId, cluster);
        if (!addedRoleToUser) return;

        NotificationService.notify(`Added new role to user!`, 'Success!', 'success');

        NotificationService.notify(`Registration complete!`, 'Success!', 'success');

        saveLoginDetails(username, password, cluster, newNode);

        setUsername('');
        setPassword('');
        setCluster('cluster0');
        setConfirmPassword('');
    };

    const canSubmit = username.length > 0 && password.length > 0 && confirmPassword === password;

    return (
        <PageContentWrapper>
            <div className="flex flex-column flex-wrap">
                <div
                    style={{ opacity: !newVNode ? 1 : 0.4 }}
                    onClick={() => {
                        setNewVNode(undefined);
                    }}
                >
                    <PageHeader withBackButton>Register V-Node</PageHeader>
                    <FormContentWrapper>
                        {/* Maybe by fetching all of the node names we can check if the "username" is available to make things smoother */}
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
                        <PasswordInput
                            required
                            name="confirm_password"
                            label={'Confirm Password*'}
                            value={confirmPassword}
                            onChange={handleInputChange}
                            invalid={confirmPassword !== password || confirmPassword.length === 0}
                        />
                        <Select value={cluster} label="Cluster" onChange={(event) => setCluster(event.target.value)}>
                            <Option value="cluster0">Cluster 0</Option>
                            <Option value="cluster1">Cluster 1</Option>
                            <Option value="cluster2">Cluster 2</Option>
                        </Select>
                        <Button
                            style={{ width: 142 }}
                            className="h-12"
                            size={'large'}
                            variant={'primary'}
                            disabled={!canSubmit}
                            onClick={() => {
                                trackPromise(handleSubmit());
                            }}
                        >
                            Register
                        </Button>
                    </FormContentWrapper>
                </div>
                {newVNode && (
                    <div className="sm:ml-24 md:ml-2 lg:ml-2">
                        <PageHeader>Your own VNode!</PageHeader>
                        <div
                            className="shadow-2xl ml-4 mt-8 p-12"
                            style={{
                                marginTop: 8,
                                border: '1px solid lightgrey',
                                padding: 12,
                                maxWidth: 400,
                                borderRadius: 12,
                                background: 'white',
                            }}
                        >
                            <div>
                                <p>
                                    <strong>x500 Name:</strong> {newVNode.holdingIdentity.x500Name}
                                </p>
                                <p>
                                    <strong>Group ID:</strong> {newVNode.holdingIdentity.groupId}
                                </p>
                                <p>
                                    <strong>Holding ID:</strong> {newVNode.holdingIdentity.shortHash}
                                </p>
                                <p>
                                    <strong>Cpi : </strong>
                                    {newVNode.cpiIdentifier.name}
                                </p>
                            </div>
                            <Button className="h-12 mt-6" size={'large'} variant={'primary'} onClick={handleVNodeLogin}>
                                Login
                            </Button>
                        </div>
                    </div>
                )}
            </div>
            <VisualizationWrapper width={700}>
                <RegisterViz />
            </VisualizationWrapper>
        </PageContentWrapper>
    );
};

export default Register;
