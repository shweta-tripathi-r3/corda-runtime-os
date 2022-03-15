import { Button, TextField, Typography } from '@material-ui/core';
import { ChangeEvent, useState } from 'react';

import { Cpi } from 'model/cpi';
import { requestCreateNode } from 'api/vnode';
import useAppDataContext from 'contexts/AppDataContext';
import { useAppStyles } from 'materialStyles/appStyles';
import { useSnackbar } from 'notistack';

type Props = {
    cpi: Cpi;
    handleBackButton: () => void;
};

const CreateVNode: React.FunctionComponent<Props> = ({ cpi, handleBackButton }) => {
    const appClasses = useAppStyles();
    const [x500, setX500] = useState<string>('');
    const { refreshVNodes } = useAppDataContext();
    const { enqueueSnackbar } = useSnackbar();

    const createNode = async () => {
        const response = await requestCreateNode(cpi.fileChecksum, x500);

        if (response.data) {
            enqueueSnackbar(`V Node ${response.data.x500Name} successfully created.`, { variant: 'success' });
        }
        if (response.error) {
            enqueueSnackbar(`Failed to create V Node, Error: ${response.error}.`, { variant: 'error' });
        }
        handleBackButton();
        setTimeout(() => {
            refreshVNodes();
        }, 2000);
    };

    const handleNodeNameChange = (e: ChangeEvent<HTMLInputElement>) => {
        setX500(e.target.value);
    };

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Typography variant="h4" className={appClasses.contrastText} style={{ marginBottom: 16 }}>
                Create V Node
            </Typography>
            <Typography variant="h6" className={appClasses.secondaryLightText} style={{ fontWeight: 600 }}>
                Cpi: {cpi.id.cpiName}
            </Typography>
            <Typography className={appClasses.contrastText}>V Node x500 Name</Typography>
            <TextField
                id={'x500Name'}
                color="secondary"
                className={appClasses.textInput}
                variant="outlined"
                onChange={handleNodeNameChange}
                placeholder={'e.g CN=Alice, O=Alice Corp, L=LDN, C=GB'}
            />
            <div style={{ display: 'flex', gap: 12 }}>
                <Button className={appClasses.button} variant="outlined" color="secondary" onClick={handleBackButton}>
                    Back
                </Button>
                <Button
                    id={'createVNodeSubmit'}
                    className={appClasses.button}
                    variant="outlined"
                    color="secondary"
                    onClick={createNode}
                    disabled={x500.length === 0}
                >
                    Create V Node
                </Button>
            </div>
        </div>
    );
};

export default CreateVNode;
