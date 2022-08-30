import { FULL_NAME_SPLITTER } from '@/constants/fullNameSplit';
import { NotificationService } from '@r3/r3-tooling-design-system/exports';
import adminAxiosInstance from '@/api/adminAxios';
import apiCall, { P2P_GATEWAY_PORT, P2P_GATEWAYS } from '@/api/apiCall';

export const createVNode = async (x500Name: string, cpiFileChecksum: string, cluster: string): Promise<boolean> => {
    const response = await apiCall({
        method: 'post',
        path: '/api/v1/virtualnode',
        params: {
            request: {
                cpiFileChecksum: cpiFileChecksum,
                x500Name: x500Name,
            },
        },
        axiosInstance: adminAxiosInstance[cluster],
        cluster,
    });
    if (response.error) {
        NotificationService.notify(`Failed to create VNode: Error: ${response.error}`, 'Error', 'danger');
        return false;
    } else {
        NotificationService.notify(`Successfully created VNode!`, 'Success!', 'success');
    }
    return true;
};

export const createUser = async (
    username: string,
    password: string,
    holdingShortId: string,
    cluster: string
): Promise<boolean> => {
    const response = await apiCall({
        method: 'post',
        path: '/api/v1/user',
        params: {
            createUserType: {
                enabled: true,
                fullName: `${username}${FULL_NAME_SPLITTER}${holdingShortId}`,
                initialPassword: password,
                loginName: username,
            },
        },
        axiosInstance: adminAxiosInstance[cluster],
        cluster,
    });
    if (response.error) {
        NotificationService.notify(
            `Failed to create user with username: ${username}: Error: ${response.error}`,
            'Error',
            'danger'
        );
        return false;
    } else {
        NotificationService.notify(`Successfully created user!`, 'Success!', 'success');
    }

    return true;
};

export const createPermission = async (
    permissionString: string,
    permissionType: 'DENY' | 'ALLOW',
    cluster: string
): Promise<string | undefined> => {
    const response = await apiCall({
        method: 'post',
        path: '/api/v1/permission',
        params: {
            createPermissionType: {
                permissionString: permissionString,
                permissionType: permissionType,
            },
        },
        axiosInstance: adminAxiosInstance[cluster],
        cluster,
    });
    if (response.error) {
        NotificationService.notify(
            `Failed to create permission ${permissionString} : Error: ${response.error}`,
            'Error',
            'danger'
        );
        return undefined;
    }

    return response.data.id;
};

export const createRole = async (cluster: string): Promise<string | undefined> => {
    const response = await apiCall({
        method: 'post',
        path: '/api/v1/role',
        params: {
            createRoleType: {
                roleName: 'user_role',
            },
        },
        axiosInstance: adminAxiosInstance[cluster],
        cluster,
    });
    if (response.error) {
        NotificationService.notify(`Failed to create new role: Error: ${response.error}`, 'Error', 'danger');
        return undefined;
    } else {
        //NotificationService.notify(`Successfully created new role for user!`, 'Success!', 'success');
    }

    return response.data.id;
};

export const addPermissionToRole = async (permissionId: string, roleId: string, cluster: string) => {
    const response = await apiCall({
        method: 'put',
        path: `/api/v1/role/${roleId}/permission/${permissionId}`,
        axiosInstance: adminAxiosInstance[cluster],
        cluster,
    });
    if (response.error) {
        NotificationService.notify(
            `Failed to add permission ${permissionId} to role: ${roleId} : Error: ${response.error}`,
            'Error',
            'danger'
        );
        return false;
    }

    return true;
};

export const addRoleToUser = async (loginName: string, roleId: string, cluster: string): Promise<boolean> => {
    const response = await apiCall({
        method: 'put',
        path: `/api/v1/user/${loginName}/role/${roleId}`,
        axiosInstance: adminAxiosInstance[cluster],
        cluster,
    });
    if (response.error) {
        NotificationService.notify(
            `Failed to add role ${roleId} to user: ${loginName} : Error: ${response.error}`,
            'Error',
            'danger'
        );
        return false;
    } else {
        //NotificationService.notify(`Successfully added role to new user!`, 'Success!', 'success');
    }

    return true;
};
//TODO: Check return type
export const createSessionHSM = async (holdingId: string, cluster: string): Promise<boolean> => {
    const response = await apiCall({
        method: 'post',
        path: `/api/v1/hsm/soft/${holdingId}/SESSION_INIT`,
        axiosInstance: adminAxiosInstance[cluster],
        cluster,
    });
    if (response.error) {
        NotificationService.notify(
            `Failed to create session HSM for ${holdingId} : Error: ${response.error}`,
            'Error',
            'danger'
        );
        return false;
    }

    return true;
};

export const generateSessionKey = async (holdingId: string, cluster: string): Promise<string | undefined> => {
    const response = await apiCall({
        method: 'post',
        path: `/api/v1/keys/${holdingId}/alias/${holdingId}-session/category/SESSION_INIT/scheme/CORDA.ECDSA.SECP256R1`,
        axiosInstance: adminAxiosInstance[cluster],
        cluster,
    });
    if (response.error) {
        NotificationService.notify(
            `Failed to generate session key for ${holdingId} : Error: ${response.error}`,
            'Error',
            'danger'
        );
        return undefined;
    }

    return response.data;
};

export const generateLedgerHSM = async (holdingId: string, cluster: string): Promise<boolean> => {
    const response = await apiCall({
        method: 'post',
        path: `/api/v1/hsm/soft/${holdingId}/LEDGER`,
        axiosInstance: adminAxiosInstance[cluster],
        cluster,
    });
    if (response.error) {
        NotificationService.notify(
            `Failed to generate ledger HSM for ${holdingId} : Error: ${response.error}`,
            'Error',
            'danger'
        );
        return false;
    }

    return true;
};

export const generateLedgerKey = async (holdingId: string, cluster: string): Promise<string | undefined> => {
    const response = await apiCall({
        method: 'post',
        path: `/api/v1/keys/${holdingId}/alias/${holdingId}-ledger/category/LEDGER/scheme/CORDA.ECDSA.SECP256R1`,
        axiosInstance: adminAxiosInstance[cluster],
        cluster,
    });
    if (response.error) {
        NotificationService.notify(
            `Failed to generate session key for ${holdingId} : Error: ${response.error}`,
            'Error',
            'danger'
        );
        return undefined;
    }

    return response.data;
};

export const networkCommunicationSetup = async (
    holdingId: string,
    sessionKey: string,
    cluster: string
): Promise<string | undefined> => {
    const response = await apiCall({
        method: 'put',
        path: `/api/v1/network/setup/${holdingId}`,
        params: { p2pTlsCertificateChainAlias: 'p2p-tls-cert', p2pTlsTenantId: 'p2p', sessionKeyId: sessionKey },
        axiosInstance: adminAxiosInstance[cluster],
        cluster,
    });
    if (response.error) {
        NotificationService.notify(`Network communication setup failed : Error: ${response.error}`, 'Error', 'danger');
        return undefined;
    }

    return response.data;
};

export const memberRegistration = async (
    holdingId: string,
    sessionKey: string,
    ledgerKey: string,
    cluster: string
): Promise<boolean> => {
    const response = await apiCall({
        method: 'post',
        path: `/api/v1/network/setup/${holdingId}`,
        params: {
            memberRegistrationRequest: {
                action: 'requestJoin',
                context: {
                    'corda.session.key.id': sessionKey,
                    'corda.ledger.keys.0.id': ledgerKey,
                    'corda.ledger.keys.0.signature.spec': 'CORDA.ECDSA.SECP256R1',
                    'corda.endpoints.0.connectionURL': `https://'${P2P_GATEWAYS[cluster]}:${P2P_GATEWAY_PORT}`,
                    'corda.endpoints.0.protocolVersion': '1',
                },
            },
        },
        axiosInstance: adminAxiosInstance[cluster],
        cluster,
    });
    if (response.error) {
        NotificationService.notify(`Network communication setup failed : Error: ${response.error}`, 'Error', 'danger');
        return false;
    }

    return true;
};
