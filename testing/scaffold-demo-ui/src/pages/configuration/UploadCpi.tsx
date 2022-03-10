import { Button, TextField, Typography } from '@material-ui/core';
import { ChangeEvent, useRef, useState } from 'react';

import { uploadCpi } from 'api/cpi';
import useAppDataContext from 'contexts/AppDataContext';
import { useAppStyles } from 'materialStyles/appStyles';

const UploadCpi = () => {
    const appClasses = useAppStyles();
    const inputRef = useRef<HTMLDivElement>(null);
    const [file, setFile] = useState<File | undefined>(undefined);
    const [x500, setX500] = useState<string>('');

    const { refreshCpiList, refreshVNodes } = useAppDataContext();

    const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files.length > 0) {
            setFile(e.target.files[0]);
        }
    };

    const handleNodeNameChange = (e: ChangeEvent<HTMLInputElement>) => {
        setX500(e.target.value);
    };

    const upload = async () => {
        if (!file) return;

        const cpiUploadResponse = await uploadCpi(file?.name, file);

        refreshCpiList();
        refreshVNodes();
    };

    return (
        <div style={{ width: 350, display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Typography variant="h4" className={appClasses.contrastText} style={{ marginBottom: 16 }}>
                Upload CPI
            </Typography>
            <Button
                className={appClasses.button}
                component="label"
                variant="outlined"
                color="secondary"
                onClick={() => {
                    if (!inputRef) return;
                    inputRef.current?.click();
                }}
            >
                Choose File
                <input type="file" hidden onChange={handleInputChange} />
            </Button>

            {file && <Typography className={appClasses.secondaryText}>{file.name}</Typography>}

            <Button
                className={appClasses.button}
                variant="outlined"
                color="secondary"
                onClick={upload}
                disabled={file === undefined}
            >
                Upload
            </Button>
        </div>
    );
};

export default UploadCpi;
