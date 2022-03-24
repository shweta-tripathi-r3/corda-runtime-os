import { Button, Typography } from '@material-ui/core';
import { ChangeEvent, useRef, useState } from 'react';
import { getCpiStatus, uploadCpi } from 'api/cpi';

import useAppDataContext from 'contexts/AppDataContext';
import { useAppStyles } from 'materialStyles/appStyles';
import { useSnackbar } from 'notistack';

const UploadCpi = () => {
    const appClasses = useAppStyles();
    const inputRef = useRef<HTMLDivElement>(null);
    const [file, setFile] = useState<File | undefined>(undefined);
    const { refreshCpiList } = useAppDataContext();
    const { enqueueSnackbar } = useSnackbar();

    const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files.length > 0) {
            setFile(e.target.files[0]);
        }
    };

    const upload = async () => {
        if (!file) return;
        const response = await uploadCpi(file?.name, file);
        let cpiId: string | undefined = undefined;
        if (response.data) {
            cpiId = response.data.id;
            enqueueSnackbar(`Cpi Uploaded with ID ${response.data.id}.`, { variant: 'success' });
        } else {
        }

        setTimeout(() => {
            const fetchCpiStatus = async (id: string) => {
                const response = await getCpiStatus(id);
                if (response.data.status) {
                    enqueueSnackbar(`Cpi upload status: ${response.data.status}.`, { variant: 'success' });
                }
            };

            if (cpiId) {
                fetchCpiStatus(cpiId);
            }
            refreshCpiList();
        }, 2000);
    };

    return (
        <div style={{ width: 350, display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Typography variant="h4" className={appClasses.contrastText} style={{ marginBottom: 16 }}>
                Upload CPI
            </Typography>

            {file && <Typography className={appClasses.secondaryLightText}>{file.name}</Typography>}
            <div style={{ display: 'flex', gap: 12 }}>
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
        </div>
    );
};

export default UploadCpi;
