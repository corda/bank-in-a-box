import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Card,
    CardContent,
    Divider,
    Fade,
    Paper,
    Typography,
} from '@material-ui/core';
import { Customer, EmptyCustomerData } from '../../store/types';
import React, { useEffect, useState } from 'react';

import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import { ResolvedPromise } from '../../api/resolvePromise';
import { getCustomerById } from '../../api/customerApi';
import { mapCustomerData } from '../../utils/Utils';
import { useHistory } from 'react-router-dom';
import { useInfoDisplayStyles } from '../MaterialStyles/InfoDisplayStyles';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

type Props = {
    customerId: string;
};

const CustomerDetails: React.FC<Props> = (props) => {
    const { customerId } = props;
    const history = useHistory();
    const { t } = useTranslation('common');
    const [customer, setCustomer] = useState<Customer>(EmptyCustomerData);
    const infoDisplayStyles = useInfoDisplayStyles();
    const { enqueueSnackbar } = useSnackbar();

    useEffect(() => {
        const updateCustomer = async () => {
            const customerData: ResolvedPromise = await getCustomerById(customerId);
            if (customerData.error) {
                enqueueSnackbar(t('common:error.serverContactError', { error: customerData.error }), {
                    variant: 'error',
                });
                history.push('/');
            } else {
                const customerMapped = mapCustomerData(customerData.data.data);
                setCustomer(customerMapped);
            }
        };
        updateCustomer();
    }, [customerId, history, t, enqueueSnackbar]);

    return (
        <Fade in={true}>
            <Paper elevation={3} className={infoDisplayStyles.infoDisplay}>
                <div className={infoDisplayStyles.column}>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:commonText.customerName')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {customer.customerName}
                            </Typography>
                        </CardContent>
                    </Card>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:commonText.contactNumber')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {customer.contactNumber}
                            </Typography>
                        </CardContent>
                    </Card>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:commonText.emailAddress')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {customer.emailAddress}
                            </Typography>
                        </CardContent>
                    </Card>
                </div>

                <div className={infoDisplayStyles.column}>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:commonText.postCode')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {customer.postCode}
                            </Typography>
                        </CardContent>
                    </Card>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:customer.createdOnDate')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {customer.createdOn}
                            </Typography>
                        </CardContent>
                    </Card>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:customer.modifiedOnDate')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {customer.modifiedOn}
                            </Typography>
                        </CardContent>
                    </Card>
                </div>
                <div className={infoDisplayStyles.column}>
                    <Accordion defaultExpanded={true} className={infoDisplayStyles.card}>
                        <AccordionSummary
                            expandIcon={<ExpandMoreIcon />}
                            aria-controls="panel1a-content"
                            id="panel1a-header"
                        >
                            <Typography>{t('common:customer.attachments')}</Typography>
                        </AccordionSummary>
                        {customer.attachments.map((attachment, index) => {
                            return (
                                <>
                                    <Divider />
                                    <AccordionDetails key={index}>
                                        <Typography>{attachment.name}</Typography>
                                    </AccordionDetails>
                                </>
                            );
                        })}
                    </Accordion>
                </div>
            </Paper>
        </Fade>
    );
};

export default CustomerDetails;
