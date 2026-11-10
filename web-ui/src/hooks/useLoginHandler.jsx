
import { useNavigate } from 'react-router-dom';
import useFullParams from './useFullParams';
import { useRedirect } from './useRedirect';
import { useAuth } from '../router/AuthProvider';


const useLoginHandler = () => {

    const navigate = useNavigate()

    const { targetUrl } = useFullParams()

    const redirect = useRedirect()

    const { signin } = useAuth()

    const processLoginSuccess = async (tokenResponse, externalTargetUrl) => {

        const finalTargetUrl = externalTargetUrl || targetUrl || '/'

        if (tokenResponse.status === 'SUCCESS') {
            await signin()
            redirect(finalTargetUrl)
            return
        }

        if (tokenResponse.status === 'MFA_REQUIRED') {
            const { mfaTicket, preferredMfaType, availableMfaTypes } = tokenResponse
            let mfaPath = '/login/mfa'
            if (finalTargetUrl && finalTargetUrl !== '/') {
                mfaPath += `?targetUrl=${encodeURIComponent(finalTargetUrl)}`;
            }
            navigate(mfaPath, {
                state: {
                    ticket: mfaTicket,
                    preferredMfaType: preferredMfaType,
                    availableMfaTypes: availableMfaTypes
                }
            })
            return
        }

    }

    return { processLoginSuccess }

}

export default useLoginHandler