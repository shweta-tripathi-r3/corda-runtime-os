import { Button, TextField, Typography } from '@material-ui/core';
import { ChangeEvent, useState } from 'react';

import { Cpi } from 'model/cpi';
import { requestCreateNode } from 'api/vnode';
import { useAppStyles } from 'materialStyles/appStyles';

type Props = {
    cpi: Cpi;
    handleBackButton: () => void;
};

const CreateVNode: React.FunctionComponent<Props> = ({ cpi, handleBackButton }) => {
    const appClasses = useAppStyles();
    const [x500, setX500] = useState<string>('');

    const createNode = async () => {
        const response = await requestCreateNode(cpi.fileChecksum, x500);
        handleBackButton();
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
