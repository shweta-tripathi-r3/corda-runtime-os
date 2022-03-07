import { Button } from '@material-ui/core';
import { Link as RouterLink } from 'react-router-dom';
import { useNavButtonStyles } from 'materialStyles/navButtonStyles';

type Props = {
    icon: React.ReactNode;
    label: string;
    path: string;
};

const NavButton: React.FunctionComponent<Props> = ({ path, icon, label }) => {
    const classes = useNavButtonStyles();

    return (
        <Button
            className={`navButton  ${classes.buttonNav} ${window.location.pathname === path ? classes.selected : ''}`}
            startIcon={icon}
            color="primary"
            component={RouterLink}
            {...{ to: path }}
        >
            {label}
        </Button>
    );
};

export default NavButton;
