import { useLocation, useNavigate } from "react-router-dom"
import { useDispatch } from 'react-redux';
import { removeTabItem } from "../redux/slices/layoutSlice";

const useBack = () => {

    const dispatch = useDispatch()

    const location = useLocation()

    const navigate = useNavigate()

    const goBack = () => {
        navigate(-1)
        dispatch(removeTabItem({ targetKey: location.pathname, selectKey: null }))
    }


    return { goBack }
}

export default useBack