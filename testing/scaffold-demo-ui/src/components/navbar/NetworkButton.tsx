import AccountTreeRoundedIcon from '@material-ui/icons/AccountTreeRounded';
import { Button } from '@material-ui/core';
import { useTopNavWrapperStyles } from '../../materialStyles/topNavWrapperStyles';

const NetworkButton = () => {
    const classes = useTopNavWrapperStyles();
    return (
        <Button
            className={`navButton  ${classes.buttonNav} ${
                window.location.pathname === '/' || window.location.pathname === '/' ? classes.selected : ''
            }`}
            startIcon={<AccountTreeRoundedIcon />}
            color="primary"
            href={'/'}
        >
            {'Network'}
        </Button>
    );
};

export default NetworkButton;
